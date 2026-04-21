package ru.car.service.message.telegram.render;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelegramMessagesTest {

    private final TelegramMessages messages = new TelegramMessages(buildSource());

    @Test
    void returnsSimpleKey() {
        // tg.home.btn.qrs was superseded by 2.2; use a stable, non-overridden key
        assertThat(messages.get("tg.auth.btn.share_contact")).isEqualTo("поделиться контактом");
    }

    @Test
    void formatsTemplateWithArgs() {
        String result = messages.get("tg.temp_qr.created", "abc-123", "https://car-id.ru", "abc-123");
        assertThat(result).contains("abc-123", "https://car-id.ru");
    }

    @Test
    void throwsOnMissingKey() {
        assertThatThrownBy(() -> messages.get("tg.nonexistent.key"))
                .isInstanceOf(org.springframework.context.NoSuchMessageException.class);
    }

    @Test
    void all2_2KeysPresent() {
        String[] required = {
            "tg.home.title", "tg.home.btn.notifications", "tg.home.btn.qrs", "tg.home.btn.temp_qr",
            "tg.home.btn.settings", "tg.home.btn.profile",
            "tg.qr.list.title", "tg.qr.list.card.active", "tg.qr.list.empty_v2",
            "tg.qr.details.title", "tg.qr.details.fields", "tg.qr.details.confirm.disable",
            "tg.notif.list.title", "tg.notif.list.pagination", "tg.notif.list.empty",
            "tg.settings.title", "tg.settings.body", "tg.settings.on",
            "tg.profile.title", "tg.profile.body", "tg.profile.confirm.delete",
            "tg.common.back", "tg.common.home", "tg.common.confirm.yes",
            "tg.temp_qr.caption"
        };
        for (String key : required) {
            assertThat(messages.get(key)).as("key %s", key).isNotBlank();
        }
    }

    private static ReloadableResourceBundleMessageSource buildSource() {
        var source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:i18n/telegram");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(false);
        return source;
    }
}
