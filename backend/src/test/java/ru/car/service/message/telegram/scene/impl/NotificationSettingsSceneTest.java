package ru.car.service.message.telegram.scene.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import ru.car.model.NotificationSetting;
import ru.car.repository.NotificationSettingRepository;
import ru.car.service.NotificationSettingService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSettingsSceneTest {

    @Mock NotificationSettingService settingService;
    @Mock NotificationSettingRepository settingRepository;

    NotificationSettingsScene scene;

    @BeforeEach
    void setUp() {
        scene = new NotificationSettingsScene(settingService, settingRepository, messages());
    }

    @Test
    void keyIsSettings() {
        assertThat(scene.key()).isEqualTo("settings");
    }

    @Test
    void handle_open_rendersWithCurrentState() {
        NotificationSetting setting = NotificationSetting.builder()
            .pushEnabled(true).callEnabled(false).telegramEnabled(true).build();
        when(settingRepository.findByUserId(42L)).thenReturn(setting);

        SceneOutput output = scene.handle(
            new CallbackData("settings", "open", List.of()),
            new TelegramUpdateContext(100L, 42L, null, null));

        assertThat(output.text()).contains("Настройки уведомлений");
        assertThat(output.editInPlace()).isTrue();
    }

    @Test
    void handle_toggle_delegatesAndRerenders() {
        NotificationSetting toggled = NotificationSetting.builder()
            .pushEnabled(false).callEnabled(false).telegramEnabled(true).build();
        when(settingService.toggleChannel(42L, "push")).thenReturn(toggled);
        when(settingRepository.findByUserId(42L)).thenReturn(toggled);

        SceneOutput output = scene.handle(
            new CallbackData("settings", "toggle", List.of("push")),
            new TelegramUpdateContext(100L, 42L, null, null));

        verify(settingService).toggleChannel(42L, "push");
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
