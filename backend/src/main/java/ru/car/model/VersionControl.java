package ru.car.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionControl {
    private Long id;
    private String appleCurrent;
    private String appleMin;
    private String googleCurrent;
    private String googleMin;
    private String rustoreCurrent;
    private String rustoreMin;
}
