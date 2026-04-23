package ru.car.service.message.telegram.scene.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.car.service.NotificationService;
import ru.car.service.QrService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeSceneTest {

    @Mock NotificationService notificationService;
    @Mock QrService qrService;

    HomeScene scene;

    @BeforeEach
    void setUp() {
        scene = new HomeScene(notificationService, qrService, messages());
    }

    @Test
    void keyIsHome() {
        assertThat(scene.key()).isEqualTo("home");
    }

    @Test
    void render_withCounts_buildsEightButtons() {
        when(notificationService.countUnreadByUserId(42L)).thenReturn(2);
        when(qrService.countByUserId(42L)).thenReturn(3);

        SceneOutput output = scene.render(new TelegramUpdateContext(100L, 42L, null, null));

        assertThat(output.text()).contains("Car ID");
        assertThat(output.parseMode()).isEqualTo("HTML");
        InlineKeyboardMarkup kb = output.inlineKeyboard();
        assertThat(kb).isNotNull();
        assertThat(kb.getKeyboard()).hasSize(8);
    }

    @Test
    void render_includesReportSupportMarketplaceButtons() {
        when(notificationService.countUnreadByUserId(42L)).thenReturn(0);
        when(qrService.countByUserId(42L)).thenReturn(0);

        SceneOutput output = scene.render(new TelegramUpdateContext(100L, 42L, null, null));

        String allCallbacks = output.inlineKeyboard().getKeyboard().stream()
            .flatMap(java.util.List::stream)
            .map(b -> b.getCallbackData())
            .reduce("", (a, b) -> a + "|" + b);
        assertThat(allCallbacks).contains("report:start");
        assertThat(allCallbacks).contains("support:start");
        assertThat(allCallbacks).contains("marketplace:open");
    }

    @Test
    void render_zeroCounts_usesZeroLabelKeys() {
        when(notificationService.countUnreadByUserId(42L)).thenReturn(0);
        when(qrService.countByUserId(42L)).thenReturn(0);

        SceneOutput output = scene.render(new TelegramUpdateContext(100L, 42L, null, null));

        // zero-labels are the ones without "· N" suffix — just "Уведомления" / "Мои метки"
        InlineKeyboardMarkup kb = output.inlineKeyboard();
        String firstBtn = kb.getKeyboard().get(0).get(0).getText();
        String secondBtn = kb.getKeyboard().get(1).get(0).getText();
        assertThat(firstBtn).isEqualTo("🔔 Уведомления");
        assertThat(secondBtn).isEqualTo("🚘 Мои метки");
    }

    @Test
    void renderUnknown_hasHomeButton() {
        SceneOutput output = scene.renderUnknown(new TelegramUpdateContext(100L, 42L, null, null));
        assertThat(output.text()).contains("Неизвестная команда");
        assertThat(output.inlineKeyboard()).isNotNull();
    }

    @Test
    void handle_openCallback_rendersHomeAsEdit() {
        when(notificationService.countUnreadByUserId(42L)).thenReturn(0);
        when(qrService.countByUserId(42L)).thenReturn(0);

        SceneOutput output = scene.handle(
            new CallbackData("home", "open", List.of()),
            new TelegramUpdateContext(100L, 42L, null, null));

        assertThat(output.text()).contains("Car ID");
        assertThat(output.editInPlace()).isTrue();
    }

    private static TelegramMessages messages() {
        var src = new ReloadableResourceBundleMessageSource();
        src.setBasename("classpath:i18n/telegram");
        src.setDefaultEncoding("UTF-8");
        src.setFallbackToSystemLocale(false);
        return new TelegramMessages(src);
    }
}
