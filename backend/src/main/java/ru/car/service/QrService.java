package ru.car.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.car.dto.OwnerContactsDto;
import ru.car.dto.QrDto;
import ru.car.enums.ErrorCode;
import ru.car.enums.QrStatus;
import ru.car.exception.ForbiddenException;
import ru.car.exception.NotFoundException;
import ru.car.mapper.QrDtoMapper;
import ru.car.mapper.QrWebDtoMapper;
import ru.car.model.NotificationSetting;
import ru.car.model.Qr;
import ru.car.model.User;
import ru.car.repository.NotificationRepository;
import ru.car.repository.NotificationSettingRepository;
import ru.car.repository.QrRepository;
import ru.car.repository.UserRepository;
import ru.car.service.security.AuthService;
import ru.car.util.ContactLinks;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class QrService {
    private final QrRepository qrRepository;
    private final QrDtoMapper qrDtoMapper;
    private final QrWebDtoMapper qrWebDtoMapper;
    private final NotificationRepository notificationRepository;
    private final AuthService authService;
    private final MetricService metricService;
    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;

    public Qr findByIdOrThrowNotFound(UUID id) {
        return qrRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QR_DOES_NOT_EXIST.getDescription(), ErrorCode.QR_DOES_NOT_EXIST));
    }

    public Qr findActiveQrById(UUID id) {
        if (!qrRepository.existsByIdAndStatus(id, QrStatus.ACTIVE, QrStatus.TEMPORARY)) {
            throw new NotFoundException(ErrorCode.QR_DOES_NOT_EXIST.getDescription(), ErrorCode.QR_DOES_NOT_EXIST);
        }
        return findByIdOrThrowNotFound(id);
    }

    private Qr findByIdAndUserOrThrowNotFound(UUID id, Long userId) {
        return qrRepository.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QR_DOES_NOT_EXIST.getDescription(), ErrorCode.QR_DOES_NOT_EXIST));
    }



    @Transactional
    public QrDto getQrById(QrDto dto) {
        return qrDtoMapper.toDto(findByIdOrThrowNotFound(dto.getQrId()));
    }

    @Transactional
    public QrDto getQrById(UUID id) {
        Qr qr = findByIdOrThrowNotFound(id);
        QrDto dto = qrWebDtoMapper.toWebDto(qr);
        dto.setOwnerContacts(resolveOwnerContacts(qr));
        return dto;
    }

    /** BF6: опубликованные владельцем контакты для развилки на скане (null, если ничего не задано). */
    private OwnerContactsDto resolveOwnerContacts(Qr qr) {
        if (qr.getUserId() == null) {
            return null;
        }
        NotificationSetting setting = notificationSettingRepository.findByQrId(qr.getId());
        if (setting == null) {
            return null;
        }
        String phone = null;
        if (Boolean.TRUE.equals(setting.getShowPhoneOnUnreachable())) {
            phone = userRepository.findById(setting.getUserId())
                    .map(User::getPhoneNumber)
                    .filter(p -> p != null && !p.isBlank())
                    .map(p -> "+" + p)
                    .orElse(null);
        }
        OwnerContactsDto contacts = OwnerContactsDto.builder()
                .phone(phone)
                .telegram(ContactLinks.telegram(setting.getTelegramContact()))
                .vk(ContactLinks.vk(setting.getVkContact()))
                .max(ContactLinks.max(setting.getMaxContact()))
                .build();
        boolean empty = contacts.getPhone() == null && contacts.getTelegram() == null
                && contacts.getVk() == null && contacts.getMax() == null;
        return empty ? null : contacts;
    }

    @Transactional
    public QrDto createQr() {
        return qrDtoMapper.toDto(qrRepository.save(Qr.builder()
                .batchId(1L)
                .printed(false)
                .status(QrStatus.NEW)
                .build()));
    }

    @Transactional
    public QrDto createTemporaryQr(Long userId) {
        if (!qrRepository.findByUserId(userId).isEmpty()) {
            throw new NotFoundException(ErrorCode.ALREADY_HAS_QR.getDescription(), ErrorCode.ALREADY_HAS_QR);
        }

        Qr save = qrRepository.save(Qr.builder()
                .batchId(1L)
                .printed(false)
                .status(QrStatus.TEMPORARY)
                .name("Временный qr")
                .userId(userId)
                .build());
        return qrDtoMapper.toDto(save);
    }

    @Transactional
    public QrDto linkToUser(QrDto dto) {
        Long userId = authService.getUserId();
        Qr qr = findByIdOrThrowNotFound(dto.getQrId());
        if (QrStatus.ACTIVE.equals(qr.getStatus())) {
            throw new NotFoundException(ErrorCode.ALREADY_ACTIVE_QR.getDescription(), ErrorCode.ALREADY_ACTIVE_QR);
        }
        if (QrStatus.TEMPORARY.equals(qr.getStatus())) {
            throw new ForbiddenException(ErrorCode.TEMPORARY_QR.getDescription(), ErrorCode.TEMPORARY_QR);
        }
        boolean isNewQr = QrStatus.NEW.equals(qr.getStatus());
        qr.setUserId(userId);
        qr.setName(dto.getQrName());
        qr.setStatus(QrStatus.ACTIVE);
        qrRepository.updateUserIdAndStatusAndName(qr);

        if (isNewQr) {
            metricService.activateQr();
        }
        return qrDtoMapper.toDto(qr);
    }

    @Transactional
    public List<QrDto> getAll() {
        Long userId = authService.getUserId();
        return qrDtoMapper.toDto(qrRepository.findByUserId(userId));
    }

    @Transactional
    public QrDto delete(QrDto dto) {
        Long userId = authService.getUserId();
        Qr qr = findByIdAndUserOrThrowNotFound(dto.getQrId(), userId);
        if (QrStatus.TEMPORARY.equals(qr.getStatus())) {
            throw new ForbiddenException(ErrorCode.TEMPORARY_QR.getDescription(), ErrorCode.TEMPORARY_QR);
        }
        qr.setStatus(QrStatus.DELETED);
        qrRepository.delete(qr);
        notificationRepository.deleteByQrId(qr.getId());
        return qrDtoMapper.toDto(qr);
    }

    @Transactional
    public void disable(UUID id) {
        Qr qr = findByIdOrThrowNotFound(id);
        qr.setStatus(QrStatus.DELETED);
        qrRepository.delete(qr);
    }

    @Transactional(readOnly = true)
    public int countByUserId(Long userId) {
        return (int) qrRepository.findByUserId(userId).stream()
                .filter(qr -> !QrStatus.DELETED.equals(qr.getStatus()))
                .count();
    }

    @Transactional
    public void deleteAllByUserId(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
        qrRepository.destroyTemporaryByUserId(userId);
        qrRepository.deleteAllByUserId(userId);
    }

    @Transactional
    public List<UUID> destroyAllTemporaryQr() {
        LocalDateTime period = LocalDateTime.now().minusHours(1L);
        List<UUID> ids = qrRepository.findByStatusBefore(QrStatus.TEMPORARY, period).stream()
                .map(Qr::getId)
                .collect(Collectors.toList());
        ids.forEach(id -> {
            notificationRepository.deleteByQrId(id);
            qrRepository.destroy(id);
        });
        return ids;
    }
}
