package ru.car.service.message.telegram.render;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelegramMessagesTest {

    private final TelegramMessages messages = new TelegramMessages(buildSource());

    @Test
    void returnsSimpleKey() {
        assertThat(messages.get("tg.home.btn.qrs")).isEqualTo("QR-коды");
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

    private static ReloadableResourceBundleMessageSource buildSource() {
        var source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:i18n/telegram");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(false);
        return source;
    }
}
