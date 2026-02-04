package ru.car.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.car.enums.BatchTemplates;
import ru.car.enums.DistributionMethods;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Batch {
    private Long id;
    private BatchTemplates template;
    private LocalDateTime createdDate;
    private DistributionMethods distributionMethod;
    private String address;
    private String description;
}
