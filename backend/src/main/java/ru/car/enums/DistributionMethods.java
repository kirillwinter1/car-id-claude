package ru.car.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DistributionMethods {
    NOT_SPECIFIED("NOT_SPECIFIED", "не задано"),
    MARKETPLACE("MARKETPLACE", "маркетплейс"),
    CAR_WASH("CAR_WASH", "мойка машин");

    private final String code;
    private final String description;

}
