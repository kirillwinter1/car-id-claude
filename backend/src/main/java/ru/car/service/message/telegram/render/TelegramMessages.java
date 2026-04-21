package ru.car.service.message.telegram.render;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class TelegramMessages {

    private static final Locale RU = new Locale("ru", "RU");

    private final MessageSource messageSource;

    public TelegramMessages(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String get(String key, Object... args) {
        return messageSource.getMessage(key, args, RU);
    }
}
