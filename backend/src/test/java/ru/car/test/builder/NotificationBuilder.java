package ru.car.test.builder;

import ru.car.enums.NotificationStatus;
import ru.car.model.Notification;
import ru.car.model.Qr;
import ru.car.model.ReasonDictionary;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Builder for creating Notification instances in tests.
 */
public class NotificationBuilder {

    private UUID id = UUID.randomUUID();
    private UUID qrId = UUID.randomUUID();
    private Qr qr = null;
    private Long reasonId = 1L;
    private ReasonDictionary reason = null;
    private String text = null;
    private Long senderId = null;
    private String visitorId = null;
    private LocalDateTime createdDate = LocalDateTime.now();
    private NotificationStatus status = NotificationStatus.DRAFT;

    public static NotificationBuilder aNotification() {
        return new NotificationBuilder();
    }

    public NotificationBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public NotificationBuilder withQrId(UUID qrId) {
        this.qrId = qrId;
        return this;
    }

    public NotificationBuilder withQr(Qr qr) {
        this.qr = qr;
        this.qrId = qr.getId();
        return this;
    }

    public NotificationBuilder withReasonId(Long reasonId) {
        this.reasonId = reasonId;
        return this;
    }

    public NotificationBuilder withReason(ReasonDictionary reason) {
        this.reason = reason;
        this.reasonId = reason.getId();
        return this;
    }

    public NotificationBuilder withText(String text) {
        this.text = text;
        return this;
    }

    public NotificationBuilder withSenderId(Long senderId) {
        this.senderId = senderId;
        return this;
    }

    public NotificationBuilder withVisitorId(String visitorId) {
        this.visitorId = visitorId;
        return this;
    }

    public NotificationBuilder withCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public NotificationBuilder withStatus(NotificationStatus status) {
        this.status = status;
        return this;
    }

    public NotificationBuilder asDraft() {
        this.status = NotificationStatus.DRAFT;
        return this;
    }

    public NotificationBuilder asUnread() {
        this.status = NotificationStatus.UNREAD;
        return this;
    }

    public NotificationBuilder asRead() {
        this.status = NotificationStatus.READ;
        return this;
    }

    public NotificationBuilder asSend() {
        this.status = NotificationStatus.SEND;
        return this;
    }

    public NotificationBuilder createdMinutesAgo(int minutes) {
        this.createdDate = LocalDateTime.now().minusMinutes(minutes);
        return this;
    }

    public Notification build() {
        return Notification.builder()
                .id(id)
                .qrId(qrId)
                .qr(qr)
                .reasonId(reasonId)
                .reason(reason)
                .text(text)
                .senderId(senderId)
                .visitorId(visitorId)
                .createdDate(createdDate)
                .status(status)
                .build();
    }
}
