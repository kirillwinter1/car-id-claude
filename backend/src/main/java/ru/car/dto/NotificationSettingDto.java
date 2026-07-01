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
    private Boolean active;
    @JsonProperty("telegram_dialog_id")
    private Long telegramDialogId;
    /** BF5: показывать ли номер владельца прохожему при неответе. */
    @JsonProperty("show_phone_on_unreachable")
    private Boolean showPhoneOnUnreachable;
    /** BF6: публичные контакты владельца (сырой ввод). */
    @JsonProperty("telegram_contact")
    private String telegramContact;
    @JsonProperty("vk_contact")
    private String vkContact;
    @JsonProperty("max_contact")
    private String maxContact;
}
