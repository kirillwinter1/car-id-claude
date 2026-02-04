package ru.car.dto.web.requestTypes;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDtoRqWeb {
//    @Schema(description = "Тип связи [WEB, APP, TELEGRAM]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
//    private FeedbackChannels channel;
    @Schema(description = "Адрес электронной почты", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
    @Schema(description = "Текст фидбека", requiredMode = Schema.RequiredMode.REQUIRED)
    private String text;
}
