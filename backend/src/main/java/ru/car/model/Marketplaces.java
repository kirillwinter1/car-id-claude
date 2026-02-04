package ru.car.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Marketplaces {
    private Long id;
    private String wb;
    private String ozon;
    private Boolean activity;
}
