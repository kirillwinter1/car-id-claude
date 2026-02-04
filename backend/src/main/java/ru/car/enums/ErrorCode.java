package ru.car.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    EMPTY_CODE(null, null),
    AUTH_MOBILE_CODE_NOT_RIGHT("AUTH_MOBILE_CODE_NOT_RIGHT", "Введен неправильный код подтверждения"),
    BAD_REQUEST("BAD_REQUEST", "Неверный формат запроса"),
    FORBIDDEN_ERROR("FORBIDDEN_ERROR", "Доступ запрещен"),
    ALREADY_ACTIVE_QR("ALREADY_ACTIVE_QR", "QR код уже активирован"),
    ALREADY_READ_NOTIFICATION("ALREADY_READ_NOTIFICATION", "Уведомление уже прочитано"),
    TEMPORARY_QR("TEMPORARY_QR", "Функционал недоступен для временного QR кода"),
    ALREADY_HAS_QR("ALREADY_HAS_QR", "У вас уже есть QR коды"),
    QR_DOES_NOT_EXIST("QR_DOES_NOT_EXIST", "QR код никому не принадлежит"),
    INVALID_QR("INVALID_QR", "Некорректный QR-code"),
    INVALID_NOTIFICATION_ID("INVALID_NOTIFICATION_ID", "Некорректный id уведомления"),
    INVALID_ID("INVALID_ID", "Некорректный id"),
    SENDER_NOT_SET("SENDER_NOT_SET", "Не указан отправитель"),
    REQUEST_EXHAUSTED("REQUEST_EXHAUSTED", "Вы превысили лимит запросов (2 в час). Подождите или установите приложение Car ID из AppStore или RuStore"),
    UNKNOWN_TOKEN("UNKNOWN_TOKEN", "Получен неизвестный токен"),
    SEND_TIMEOUT("SEND_TIMEOUT", "Отправка следующего события возможна через %s сек."),
    TELEGRAM_AUTH_ERROR("TELEGRAM_AUTH_ERROR", "Авторизуйтесь в телеграм боте @%s, нажав кнопку %s или написав свой телефон текстом"),
    WHATSAPP_AUTH_ERROR("WHATSAPP_AUTH_ERROR", "Указанный телефон не зарегистрирован в сервисе Whatsapp"),
    SMS_ALREADY_SENT("SMS_ALREADY_SENT", "");

    private final String code;
    private final String description;

}
