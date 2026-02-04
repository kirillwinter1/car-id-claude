package ru.car.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.car.enums.NotificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private UUID id;
    private UUID qrId;
    private Qr qr;
    private Long reasonId;
    private ReasonDictionary reason;
    private String text;
    private Long senderId;
    private String visitorId;
    private LocalDateTime createdDate;
    private NotificationStatus status;
}
