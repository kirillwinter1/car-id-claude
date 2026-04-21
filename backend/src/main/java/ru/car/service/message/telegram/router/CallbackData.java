package ru.car.service.message.telegram.router;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public record CallbackData(String scene, String action, List<String> args) {

    public static Optional<CallbackData> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String[] parts = raw.split(":", -1);
        if (parts.length < 2) {
            return Optional.empty();
        }
        String scene = parts[0];
        String action = parts[1];
        List<String> args = parts.length > 2
                ? Arrays.asList(Arrays.copyOfRange(parts, 2, parts.length))
                : List.of();
        return Optional.of(new CallbackData(scene, action, args));
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder(scene).append(':').append(action);
        for (String arg : args) {
            sb.append(':').append(arg);
        }
        return sb.toString();
    }
}
