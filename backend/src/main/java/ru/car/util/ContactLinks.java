package ru.car.util;

/** BF6: строит публичные ссылки на мессенджеры владельца из сырого ввода. Пусто → null. */
public final class ContactLinks {
    private ContactLinks() {}

    public static String telegram(String raw) {
        if (isBlank(raw)) return null;
        String h = raw.trim()
                .replaceFirst("^@", "")
                .replaceFirst("(?i)^https?://(t\\.me|telegram\\.me)/", "");
        return h.isBlank() ? null : "https://t.me/" + h;
    }

    public static String vk(String raw) {
        if (isBlank(raw)) return null;
        String h = raw.trim().replaceFirst("(?i)^https?://(m\\.)?(vk\\.com|vk\\.me)/", "");
        return h.isBlank() ? null : "https://vk.me/" + h;
    }

    public static String max(String raw) {
        if (isBlank(raw)) return null;
        String r = raw.trim();
        return r.matches("(?i)^https?://.*") ? r : "https://" + r;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
