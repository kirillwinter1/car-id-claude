package ru.car.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.car.enums.QrStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Qr {
    private UUID id;
    private Long seqNumber;
    private Long batchId;
    private String name;
    private Boolean printed;
    private QrStatus status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private LocalDateTime activateDate;
    private LocalDateTime deleteDate;
    private Long userId;
}
