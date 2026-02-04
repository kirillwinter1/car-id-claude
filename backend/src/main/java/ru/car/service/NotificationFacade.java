package ru.car.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.car.constants.ApplicationConstants;
import ru.car.dto.NotificationDto;
import ru.car.enums.NotificationStatus;
import ru.car.service.message.MessageService;
import ru.car.service.security.AuthService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class NotificationFacade {
    private final NotificationService notificationService;
    private final MessageService messageService;
    private final AuthService authService;

    public NotificationDto read(NotificationDto dto) {
        Long userId = authService.getUserId();
        return readBy(dto, userId);
    }

    public NotificationDto readBy(NotificationDto dto, Long userId) {
        NotificationDto notification = notificationService.read(dto, userId);
        LocalDateTime date = LocalDateTime.of(LocalDate.now(), LocalTime.now().minusMinutes(ApplicationConstants.NOTIFICATION_LIVE_TIME_IN_MIN));
        if (notification.getTime().isAfter(date)) {
            messageService.sendReadPush(notification);
        }
        return notification;
    }

    public NotificationDto send(NotificationDto dto) {
        NotificationDto notification = notificationService.createUnread(dto);
        messageService.sendPush(notification);
        return notification;
    }

    public NotificationDto updateAndTrySendDraft(NotificationDto dto) {
        NotificationDto notification = notificationService.updateDraft(dto);
        if (NotificationStatus.SEND.equals(dto.getStatus())) {
            messageService.sendPush(notification);
        }
        return notification;
    }
}
