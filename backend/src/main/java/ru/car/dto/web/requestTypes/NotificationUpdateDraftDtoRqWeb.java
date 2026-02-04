package ru.car.dto.web.requestTypes;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class NotificationUpdateDraftDtoRqWeb {
    @JsonProperty("notification_id")
    @Schema(description = "Id нотификации", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID notificationId;
    @JsonProperty("reason_id")
    @Schema(description = "Id причины обращения")
    private Long reasonId;
    @Schema(description = "Текст обращения")
    private String text;
    @Schema(description = "Статус обращения")
    private NotificationStatus status;
}
