package ru.car.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.car.constants.ApplicationConstants;
import ru.car.enums.FeedbackChannels;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDto {
    private Long id;
    private FeedbackChannels channel;
    @Schema(description = "Адрес электронной почты", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
    @Schema(description = "Текст фидбека", requiredMode = Schema.RequiredMode.REQUIRED)
    private String text;
    @JsonProperty("created_date")
    @JsonFormat(pattern = ApplicationConstants.DATETIME_FORMAT)
    private LocalDateTime createdDate;
}
