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
import ru.car.enums.QrStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class QrDto {
    @JsonProperty("qr_id")
    @Schema(description = "Id QR-кода", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID qrId;
    @JsonProperty("seq_number")
    private Long seqNumber;
    @JsonProperty("batch_number")
    private Long batchNumber;
    @JsonProperty("qr_name")
    @Schema(description = "Название QR-кода", requiredMode = Schema.RequiredMode.REQUIRED)
    private String qrName;
    private Boolean printed;
    private QrStatus status;
    @JsonProperty("created_date")
    @JsonFormat(pattern = ApplicationConstants.DATETIME_FORMAT)
    private LocalDateTime createdDate;
    @JsonProperty("updated_date")
    @JsonFormat(pattern = ApplicationConstants.DATETIME_FORMAT)
    private LocalDateTime updatedDate;
    @JsonProperty("activate_date")
    @JsonFormat(pattern = ApplicationConstants.DATETIME_FORMAT)
    private LocalDateTime activateDate;
    @JsonProperty("delete_date")
    @JsonFormat(pattern = ApplicationConstants.DATETIME_FORMAT)
    private LocalDateTime deleteDate;
    private Long userId;
}
