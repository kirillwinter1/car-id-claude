package ru.car.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.car.enums.NotificationStatus;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatusDto {
    @JsonProperty("notification_id")
    private UUID notificationId;
    private NotificationStatus status;

    /** Запланирован ли у владельца голосовой звонок (callEnabled). Нужно фронту,
     *  чтобы честно показывать «пробуем дозвониться» только когда звонок реально будет. */
    @JsonProperty("call_enabled")
    private Boolean callEnabled;

    /** Реальный номер владельца для tel:-ссылки (BF5). Не null только если владелец разрешил
     *  показ и прошёл порог задержки. Иначе null. */
    @JsonProperty("owner_phone")
    private String ownerPhone;
}
