package ru.car.service.message.telegram.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import ru.car.model.User;
import ru.car.repository.NotificationSettingRepository;
import ru.car.service.UserService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.scene.SceneOutput;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramAuthorizationServiceTest {

    @Mock UserService userService;
    @Mock NotificationSettingRepository settingRepository;

    private TelegramAuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new TelegramAuthorizationService(userService, settingRepository, messages());
    }

    @Test
    void blankText_returnsContactRequest() {
        SceneOutput output = service.handle(100L, "");

        assertThat(output.text()).contains("Введите телефон");
        assertThat(output.replyKeyboard()).isInstanceOf(ReplyKeyboardMarkup.class);
    }

    @Test
    void invalidPhone_returnsContactRequest() {
        SceneOutput output = service.handle(100L, "not-a-phone");
        assertThat(output.text()).contains("Введите телефон");
    }

    @Test
    void alreadyLinked_returnsAlreadyLinkedText() {
        when(settingRepository.existsByTelegramDialogId(100L)).thenReturn(true);
        SceneOutput output = service.handle(100L, "+79001234567");

        assertThat(output.text()).contains("уже привязан");
    }

    @Test
    void validPhoneAndUserFound_bindsAndReturnsWelcomeWithKeyboardRemove() {
        User user = new User();
        user.setId(42L);
        when(settingRepository.existsByTelegramDialogId(100L)).thenReturn(false);
        when(userService.findByPhoneNumber("79001234567")).thenReturn(Optional.of(user));

        SceneOutput output = service.handle(100L, "+79001234567");

        verify(settingRepository).updateTelegramDialogIdByUserId(42L, 100L);
        assertThat(output.text()).contains("Рады приветствовать");
        assertThat(output.replyKeyboard()).isInstanceOf(ReplyKeyboardRemove.class);
    }

    @Test
    void validPhoneAndUserNotFound_returnsDownloadAppText() {
        when(settingRepository.existsByTelegramDialogId(100L)).thenReturn(false);
        when(userService.findByPhoneNumber("79001234567")).thenReturn(Optional.empty());

        SceneOutput output = service.handle(100L, "+79001234567");

        assertThat(output.text()).contains("Скачайте мобильное приложение");
    }

    private static TelegramMessages messages() {
        var src = new ReloadableResourceBundleMessageSource();
        src.setBasename("classpath:i18n/telegram");
        src.setDefaultEncoding("UTF-8");
        src.setFallbackToSystemLocale(false);
        return new TelegramMessages(src);
    }
}
