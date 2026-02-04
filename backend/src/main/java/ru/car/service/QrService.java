package ru.car.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.car.dto.QrDto;
import ru.car.enums.ErrorCode;
import ru.car.enums.QrStatus;
import ru.car.exception.ForbiddenException;
import ru.car.exception.NotFoundException;
import ru.car.mapper.QrDtoMapper;
import ru.car.mapper.QrWebDtoMapper;
import ru.car.model.Qr;
import ru.car.repository.NotificationRepository;
import ru.car.repository.QrRepository;
import ru.car.service.security.AuthService;

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
        return qrWebDtoMapper.toWebDto(findByIdOrThrowNotFound(id));
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
        notificationRepository.deleteAllByUserId(userId);
        return qrDtoMapper.toDto(qr);
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
