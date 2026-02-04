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
public class NotificationSetting {
    private Long id;
    private Long userId;
    private Boolean pushEnabled;
    private Boolean callEnabled;
    private Boolean telegramEnabled;
    private Boolean whatsappEnabled;
    private Boolean active;
    private Long telegramDialogId;
}
