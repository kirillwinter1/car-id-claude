package ru.car.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingDto {
    @JsonProperty("push_enabled")
    private Boolean pushEnabled;
    @JsonProperty("call_enabled")
    private Boolean callEnabled;
    @JsonProperty("telegram_enabled")
    private Boolean telegramEnabled;
    @JsonProperty("whatsapp_enabled")
    private Boolean whatsappEnabled;
    private Boolean active;
    @JsonProperty("telegram_dialog_id")
    private Long telegramDialogId;
}
