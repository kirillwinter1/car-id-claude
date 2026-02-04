package ru.car.util;

import org.springframework.jdbc.support.KeyHolder;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class KeyHolderUtils {
    public static Long getId(KeyHolder keyHolder) {
        return (Long)keyHolder.getKeys().get("id");
    }

    public static Long getQrSecNumber(KeyHolder keyHolder) {
        return (Long)keyHolder.getKeys().get("seq_number");
    }

    public static LocalDateTime getCreatedDate(KeyHolder keyHolder) {
        return ((Timestamp)keyHolder.getKeys().get("created_date")).toLocalDateTime();
    }
}
