package ru.car.service.message.telegram.scene.impl;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;

import static org.assertj.core.api.Assertions.assertThat;

class HomeMenuSceneTest {

    private final HomeMenuScene scene = new HomeMenuScene(messages());

    @Test
    void keyIsLegacyUnused() {
        // key changed to avoid SceneRegistry clash with HomeScene; will be deleted in Task 21
        assertThat(scene.key()).isEqualTo("home_legacy_unused");
    }

    @Test
    void unknownCommandRendersTextAndReplyKeyboard() {
        SceneOutput output = scene.renderUnknown(ctx());
        assertThat(output.text()).isEqualTo("Неизвестная команда");
        assertThat(output.replyKeyboard()).isInstanceOf(ReplyKeyboardMarkup.class);
        assertThat(output.editInPlace()).isFalse();
    }

    @Test
    void replyKeyboardContainsBothButtons() {
        SceneOutput output = scene.renderUnknown(ctx());
        ReplyKeyboardMarkup keyboard = (ReplyKeyboardMarkup) output.replyKeyboard();
        assertThat(keyboard.getKeyboard()).hasSize(2);
        assertThat(keyboard.getKeyboard().get(0).get(0).getText()).isEqualTo("Временный QR");
        assertThat(keyboard.getKeyboard().get(1).get(0).getText()).isEqualTo("QR-коды");
    }

    @Test
    void doesNotHandleAnyText() {
        assertThat(scene.canHandleText("QR-коды")).isFalse();
        assertThat(scene.canHandleText("Временный QR")).isFalse();
    }

    private static TelegramUpdateContext ctx() {
        return new TelegramUpdateContext(42L, 1L, null, null);
    }

    private static TelegramMessages messages() {
        var src = new ReloadableResourceBundleMessageSource();
        src.setBasename("classpath:i18n/telegram");
        src.setDefaultEncoding("UTF-8");
        src.setFallbackToSystemLocale(false);
        return new TelegramMessages(src);
    }
}
