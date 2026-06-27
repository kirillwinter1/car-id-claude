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
    private Boolean active;
    private Long telegramDialogId;
    /** BF5: показывать ли реальный номер владельца прохожему на экране статуса. По умолчанию false. */
    private Boolean showPhoneOnUnreachable;
}
