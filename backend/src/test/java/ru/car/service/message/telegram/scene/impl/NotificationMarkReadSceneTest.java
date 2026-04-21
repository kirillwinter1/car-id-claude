package ru.car.service.message.telegram.scene.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import ru.car.dto.NotificationDto;
import ru.car.model.NotificationSetting;
import ru.car.service.NotificationFacade;
import ru.car.service.NotificationService;
import ru.car.service.message.TextMessage;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationMarkReadSceneTest {

    @Mock NotificationFacade notificationFacade;
    @Mock NotificationService notificationService;

    private NotificationMarkReadScene scene;

    @BeforeEach
    void setUp() {
        scene = new NotificationMarkReadScene(notificationFacade, notificationService, telegramMessages());
    }

    @Test
    void keyIsNotif() {
        assertThat(scene.key()).isEqualTo("notif");
    }

    @Test
    void renderForTextMessage_includesMarkReadButton_whenUnread() {
        UUID id = UUID.randomUUID();
        when(notificationService.shouldShowMarkAsReadButton(id)).thenReturn(true);

        SceneOutput output = scene.renderNotification(textMessage(id, "Привет"));

        assertThat(output.text()).isEqualTo("Привет");
        assertThat(output.inlineKeyboard()).isNotNull();
        assertThat(output.inlineKeyboard().getKeyboard()).hasSize(1);
        assertThat(output.inlineKeyboard().getKeyboard().get(0).get(0).getText()).isEqualTo("отметить прочитанным");
        assertThat(output.inlineKeyboard().getKeyboard().get(0).get(0).getCallbackData()).isEqualTo("notif:read:" + id);
    }

    @Test
    void renderForTextMessage_noButton_whenAlreadyRead() {
        UUID id = UUID.randomUUID();
        when(notificationService.shouldShowMarkAsReadButton(id)).thenReturn(false);

        SceneOutput output = scene.renderNotification(textMessage(id, "Привет"));

        assertThat(output.text()).isEqualTo("Привет");
        assertThat(output.inlineKeyboard()).isNull();
    }

    @Test
    void handle_marksRead_andClearsMarkup() {
        UUID id = UUID.randomUUID();
        Long userId = 42L;
        CallbackData data = new CallbackData("notif", "read", List.of(id.toString()));

        SceneOutput output = scene.handle(data, new TelegramUpdateContext(1L, userId, null, null));

        verify(notificationFacade).readBy(NotificationDto.builder().notificationId(id).build(), userId);
        assertThat(output.editInPlace()).isTrue();
        assertThat(output.inlineKeyboard()).isNull();
    }

    @Test
    void handle_invalidUuid_noop() {
        CallbackData data = new CallbackData("notif", "read", List.of("not-a-uuid"));
        SceneOutput output = scene.handle(data, new TelegramUpdateContext(1L, 42L, null, null));

        assertThat(output.text()).isNull();
        assertThat(output.inlineKeyboard()).isNull();
    }

    private static TextMessage textMessage(UUID id, String text) {
        NotificationSetting setting = new NotificationSetting();
        setting.setTelegramDialogId(777L);
        return TextMessage.builder()
                .setting(setting)
                .notificationId(id)
                .text(text)
                .build();
    }

    private static TelegramMessages telegramMessages() {
        var src = new ReloadableResourceBundleMessageSource();
        src.setBasename("classpath:i18n/telegram");
        src.setDefaultEncoding("UTF-8");
        src.setFallbackToSystemLocale(false);
        return new TelegramMessages(src);
    }
}
