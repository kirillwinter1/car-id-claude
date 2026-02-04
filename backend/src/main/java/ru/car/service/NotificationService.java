package ru.car.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.car.dto.*;
import ru.car.enums.ErrorCode;
import ru.car.enums.NotificationStatus;
import ru.car.exception.BadRequestException;
import ru.car.exception.ForbiddenException;
import ru.car.exception.NotFoundException;
import ru.car.mapper.NotificationDtoMapper;
import ru.car.model.Notification;
import ru.car.model.Qr;
import ru.car.model.ReasonDictionary;
import ru.car.repository.NotificationRepository;
import ru.car.service.security.AuthService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationDtoMapper notificationDtoMapper;
    private final ReasonDictionaryService reasonDictionaryService;
    private final QrService qrService;
    private final AuthService authService;
    private final MetricService metricService;


    @Transactional
    public Notification findByIdOrThrowNotFound(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found"));

        LocalDateTime period = LocalDateTime.now().minusMinutes(5L);
        if (NotificationStatus.DRAFT.equals(notification.getStatus()) && notification.getCreatedDate().isBefore(period)) {
            notificationRepository.deleteById(id);
            throw new NotFoundException("Notification not found");
        }
        return notification;
    }

    @Transactional
    public NotificationDto findByIdAndSender(NotificationDto dto) {
        Notification notification = findByIdOrThrowNotFound(dto.getNotificationId());

        Long senderId = authService.getUserId();
        if (!senderId.equals(notification.getSenderId())) {
            throw new NotFoundException("Notification not found");
        }
        return notificationDtoMapper.toDto(notification);
    }

    private Notification findDraftByIdOrThrowNotFound(UUID id) {
        Notification notification = findByIdOrThrowNotFound(id);
        if (Objects.nonNull(notification.getSenderId()) || !NotificationStatus.DRAFT.equals(notification.getStatus())) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN_ERROR.getDescription(), ErrorCode.FORBIDDEN_ERROR);
        }
        return notification;
    }

    @Transactional
    public NotificationStatusDto getStatus(UUID id) {
        Notification notification = findByIdOrThrowNotFound(id);
        return NotificationStatusDto.builder()
                .notificationId(id)
                .status(notification.getStatus())
                .build();
    }

    @Transactional
    public boolean isUnread(UUID id) {
        return NotificationStatus.UNREAD.equals(findByIdOrThrowNotFound(id).getStatus());
    }

    @Transactional
    public NotificationDto read(NotificationDto dto, Long userId) {
        Notification notification = findByIdOrThrowNotFound(dto.getNotificationId());
        if (!userId.equals(notification.getQr().getUserId())) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN_ERROR.getDescription(), ErrorCode.FORBIDDEN_ERROR);
        }
        if (NotificationStatus.READ.equals(notification.getStatus())) {
            throw new ForbiddenException(ErrorCode.ALREADY_READ_NOTIFICATION.getDescription(), ErrorCode.ALREADY_READ_NOTIFICATION);
        }
        read(notification);
        return notificationDtoMapper.toDto(notification);
    }

    @Transactional
    public NotificationDto readByCallId(Long callId) {
        return notificationRepository.findByCallId(callId)
                .map(notification -> read(notification))
                .map(notification -> notificationDtoMapper.toDto(notification))
                .orElse(null);
    }

    private Notification read(Notification notification) {
        notification.setStatus(NotificationStatus.READ);
        notification = notificationRepository.updateStatus(notification);
        metricService.readNotification();
        return notification;
    }

    @Transactional
    public void updateCallId(UUID notificationId, Long callId) {
        notificationRepository.updateCallId(notificationId, callId);
    }

    @Transactional
    public NotificationDto createUnread(NotificationDto dto) {
        return create(dto, NotificationStatus.UNREAD);
    }
    @Transactional
    public NotificationDto createDraft(NotificationDto dto) {
        return create(dto, NotificationStatus.DRAFT);
    }

    private NotificationDto create(NotificationDto dto, NotificationStatus status) {
        Qr qr = qrService.findActiveQrById(dto.getQrId());
        ReasonDictionary reason = reasonDictionaryService.findByIdOrThrowNotFound(dto.getReasonId());
        Long senderId = authService.getUserIdOrNull();
        String visitorId = dto.getVisitorId();

        Notification notification = notificationRepository.save(Notification.builder()
                .qrId(dto.getQrId())
                .qr(qr)
                .reasonId(dto.getReasonId())
                .reason(reason)
                .text(null)
                .status(status)
                .senderId(senderId)
                .visitorId(visitorId)
                .build());
        return notificationDtoMapper.toDto(notification);
    }

    private boolean isValid(String visitorId) {
        return StringUtils.isNoneEmpty(visitorId) && visitorId.length() < 40;
    }

    @Transactional
    public NotificationDto updateDraft(NotificationDto dto) {
        Notification notification = findDraftByIdOrThrowNotFound(dto.getNotificationId());

        if (Objects.nonNull(dto.getReasonId()) && !dto.getReasonId().equals(notification.getReasonId())) {
            notification.setReason(reasonDictionaryService.findByIdOrThrowNotFound(dto.getReasonId()));
            notification.setReasonId(dto.getReasonId());
        }
        if (!StringUtils.isEmpty(dto.getText()) && !notification.getReason().getDescription().equals(dto.getText())) {
            notification.setText(dto.getText());
        }
        if (NotificationStatus.SEND.equals(dto.getStatus())) {
            LocalDateTime period = LocalDateTime.now().minusMinutes(1L);
            notificationRepository.findByQrIdAndDateAfter(dto.getQrId(), period).ifPresent(n -> {
                throw new BadRequestException(ErrorCode.SEND_TIMEOUT.getDescription(), ErrorCode.SEND_TIMEOUT, ChronoUnit.SECONDS.between(period, n.getCreatedDate()));
            });
            notification.setStatus(NotificationStatus.UNREAD);
        }
        notification = notificationRepository.updateReasonAndTextAndStatus(notification);
        return notificationDtoMapper.toDto(notification);
    }

    @Transactional
    public NotificationPage getAllReceived(PageParam param) {
        Long userId = authService.getUserId();
        return NotificationPage.builder()
                .page(param.getPage())
                .size(param.getSize())
                .unreadCount(notificationRepository.findCountByUserIdAndStatus(userId, NotificationStatus.UNREAD))
                .count(notificationRepository.findCountByUserId(userId))
                .notifications(notificationDtoMapper.toDto(notificationRepository.findPageByUserId(userId, param)))
                .build();
    }

    @Transactional
    public NotificationUnreadPage getAllUnreadReceived(PageParam param) {
        Long userId = authService.getUserId();
        return NotificationUnreadPage.builder()
                .page(param.getPage())
                .size(param.getSize())
                .count(notificationRepository.findCountByUserIdAndStatus(userId, NotificationStatus.UNREAD))
                .notifications(notificationDtoMapper.toDto(notificationRepository.findPageByUserIdAndStatus(userId, NotificationStatus.UNREAD, param)))
                .build();
    }

    @Transactional
    public NotificationPage getAllSend(PageParam param) {
        Long userId = authService.getUserId();
        return NotificationPage.builder()
                .page(param.getPage())
                .size(param.getSize())
                .unreadCount(notificationRepository.findCountBySenderIdAndStatus(userId, NotificationStatus.UNREAD))
                .count(notificationRepository.findCountBySenderId(userId))
                .notifications(notificationDtoMapper.toDto(notificationRepository.findPageBySenderId(userId, param)))
                .build();
    }

    @Transactional
    public NotificationUnreadPage getAllUnreadSend(PageParam param) {
        Long userId = authService.getUserId();
        return NotificationUnreadPage.builder()
                .page(param.getPage())
                .size(param.getSize())
                .count(notificationRepository.findCountBySenderIdAndStatus(userId, NotificationStatus.UNREAD))
                .notifications(notificationDtoMapper.toDto(notificationRepository.findPageBySenderIdAndStatus(userId, NotificationStatus.UNREAD, param)))
                .build();
    }

    @Transactional
    public boolean delete(NotificationDto dto) {
        return notificationRepository.deleteById(dto.getNotificationId());
    }
}
