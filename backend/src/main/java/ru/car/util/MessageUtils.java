package ru.car.util;

import org.apache.commons.lang3.StringUtils;
import ru.car.exception.BadRequestException;

import java.util.Optional;
import java.util.concurrent.Callable;

public class MessageUtils {
    public static final String MAIL_MASK = "^([a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z0-9_-]+)$";
    public static final String UUID_MASK = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$";
    public static final String PHONE_MASK = "^((8|\\+7)[- ]?)?((?\\d{3})?[- ]?)?[\\d- ]{7,10}$";


    public static boolean isUUID(String text) {
        if (StringUtils.isEmpty(text)) {
            return false;
        }
        return text.matches(UUID_MASK);
    }
    public static boolean isValidPhone(String text) {
        return getOptValidPhone(text).isPresent();
    }
    public static String getValidPhone(String text) {
        return getOptValidPhone(text)
                .orElseThrow(() -> new BadRequestException("Телефон невалиден " + text));
    }

    private static Optional<String> getOptValidPhone(String text) {
        return Optional.ofNullable(text)
                .map(String::strip)
                .filter(t -> t.matches("^((8|\\+7)[\\- ]?)?(\\(?\\d{3}\\)?[\\- ]?)?[\\d\\- ]{7,10}$"))
                .map(t -> t.replaceAll("\\D", "")
                        .replaceAll("^8", "7")
                        .replaceAll("^9", "79"));
    }

    public static Optional<String> getMail(String text) {
        return Optional.ofNullable(text)
                .map(String::strip)
                .filter(t -> t.matches(MAIL_MASK));
    }

    public static <T> T getOrNull(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Throwable ignored) {}
        return null;
    }
}
