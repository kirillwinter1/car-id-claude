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
public class PageParam {
    @Schema(description = "Номер страницы", defaultValue = "0")
    private Integer page;
    @Schema(description = "Размер страницы", defaultValue = "10")
    private Integer size;
}
