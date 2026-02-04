package ru.car.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReasonDictionaryDto {
    @Schema(description = "Id причины", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
    private String description;
}
