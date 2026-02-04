package ru.car.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.car.constants.ApplicationConstants;
import ru.car.enums.NotificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class NotificationDto {
    @JsonProperty("notification_id")
    private UUID notificationId;
    @JsonProperty("qr_id")
    @Schema(description = "Id QR-кода", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID qrId;
    @JsonProperty("qr_name")
    private String qrName;
    @JsonProperty("reason_id")
    @Schema(description = "Id причины обращения")
    private Long reasonId;
    @Schema(description = "Текст обращения")
    private String text;
    @JsonProperty("sender_id")
    private Long senderId;
    @JsonProperty("visitor_id")
    private String visitorId;
    @JsonFormat(pattern = ApplicationConstants.DATETIME_FORMAT)
    private LocalDateTime time;
    private NotificationStatus status;
}
