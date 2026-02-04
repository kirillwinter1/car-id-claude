package ru.car.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BatchTemplates {
    NOT_SPECIFIED("NOT_SPECIFIED", "шаблон не задан", "svg/stickers_1_4.svg", "default"),
    PT_WHITE_1("PT_WHITE_1", "шаблон белый 1", "svg/stickers_1_4.svg", "white"),
    PT_BLACK_1("PT_BLACK_1", "шаблон черный 1", "svg/stickers_2_4.svg", "black"),

    CSKA_1("CSKA_1", "лого команды ЦСКА", "svg/cska.svg", "cska"),
    DINAMO_1("DINAMO_1", "лого команды Динамо", "svg/dinamo.svg", "dinamo"),
    KRASNODAR_1("KRASNODAR_1", "лого команды Краснодар", "svg/krasnodar.svg", "krasnodar"),
    LOKOMOTIV_1("LOKOMOTIV_1", "лого команды Локомотив", "svg/lokomotiv.svg", "lokomotiv"),
    ROSTOV_1("ROSTOV_1", "лого команды Ростов", "svg/rostov.svg", "rostov"),
    RUBIN_1("RUBIN_1", "лого команды Рубин", "svg/rubin.svg", "rubin"),
    SPARTAK_1("SPARTAK_1", "лого команды Спартак", "svg/spartak.svg", "spartak"),
    ZENIT_1("ZENIT_1", "лого команды Зенит", "svg/zenit.svg", "zenit");

    private final String code;
    private final String description;
    private final String path;
    private final String folder;

}
