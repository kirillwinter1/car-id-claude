package ru.car.service.message.telegram.scene.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import ru.car.enums.Role;
import ru.car.model.User;
import ru.car.repository.NotificationSettingRepository;
import ru.car.service.UserService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProfileSceneTest {

    @Mock UserService userService;
    @Mock NotificationSettingRepository settingRepository;

    ProfileScene scene;

    @BeforeEach
    void setUp() {
        scene = new ProfileScene(userService, settingRepository, messages());
    }

    @Test
    void keyIsProfile() {
        assertThat(scene.key()).isEqualTo("profile");
    }

    @Test
    void handle_open_rendersProfile() {
        User user = new User();
        user.setPhoneNumber("79313178898");
        user.setRole(Role.ROLE_USER);

        SceneOutput output = scene.handle(
            new CallbackData("profile", "open", List.of()),
            new TelegramUpdateContext(100L, 42L, user, null));

        assertThat(output.text()).contains("+7 931 317-88-98");
        assertThat(output.text()).contains("Пользователь");
    }

    @Test
    void handle_logoutConfirm_clearsTelegramLink() {
        SceneOutput output = scene.handle(
            new CallbackData("profile", "logout_confirm", List.of()),
            new TelegramUpdateContext(100L, 42L, null, null));

        verify(settingRepository).clearTelegramLink(42L);
        assertThat(output.text().toLowerCase()).contains("отключены от telegram");
    }

    @Test
    void handle_deleteConfirm_callsUserServiceDelete() {
        SceneOutput output = scene.handle(
            new CallbackData("profile", "delete_confirm", List.of()),
            new TelegramUpdateContext(100L, 42L, null, null));

        verify(userService).deleteUser(42L);
        assertThat(output.text()).contains("Аккаунт удалён");
    }

    private static TelegramMessages messages() {
        var src = new ReloadableResourceBundleMessageSource();
        src.setBasename("classpath:i18n/telegram");
        src.setDefaultEncoding("UTF-8");
        src.setFallbackToSystemLocale(false);
        return new TelegramMessages(src);
    }
}
