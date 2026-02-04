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
public class MarketplacesDto {
    @Schema(description = "Ссылка на wildberries")
    private String wb;
    @Schema(description = "Ссылка на ozon")
    private String ozon;
    private Boolean activity;
}
