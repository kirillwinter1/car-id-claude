package ru.car.service.message.telegram.scene.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import ru.car.dto.NotificationDto;
import ru.car.enums.NotificationStatus;
import ru.car.model.Notification;
import ru.car.model.NotificationSetting;
import ru.car.model.Qr;
import ru.car.model.ReasonDictionary;
import ru.car.service.NotificationFacade;
import ru.car.service.NotificationService;
import ru.car.service.message.TextMessage;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;

import java.time.LocalDateTime;
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
    void renderNotification_buildsHtmlCardWithEmojiAndBothButtons() {
        UUID notifId = UUID.randomUUID();
        UUID qrId = UUID.randomUUID();
        Qr qr = new Qr();
        qr.setId(qrId);
        qr.setName("Audi Q5");
        qr.setSeqNumber(145L);
        ReasonDictionary reason = ReasonDictionary.builder().id(3L).description("Произошло ДТП").build();
        Notification n = new Notification();
        n.setId(notifId);
        n.setQrId(qrId);
        n.setQr(qr);
        n.setReasonId(3L);
        n.setReason(reason);
        n.setText("Сработала сигнализация");
        n.setCreatedDate(LocalDateTime.of(2026, 3, 23, 10, 35));
        n.setStatus(NotificationStatus.UNREAD);

        when(notificationService.findByIdOrThrowNotFound(notifId)).thenReturn(n);
        when(notificationService.shouldShowMarkAsReadButton(notifId)).thenReturn(true);

        SceneOutput out = scene.renderNotification(textMessage(notifId, "ignored"));

        assertThat(out.parseMode()).isEqualTo("HTML");
        assertThat(out.text())
            .contains("🚨")
            .contains("<b>Audi Q5</b>")
            .contains("№145")
            .contains("Произошло ДТП")
            .contains("Сработала сигнализация")
            .contains("23 марта")
            .contains("10:35");

        String callbacks = out.inlineKeyboard().getKeyboard().stream()
            .flatMap(List::stream)
            .map(org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton::getCallbackData)
            .reduce("", (a, b) -> a + "|" + b);
        assertThat(callbacks).contains("notif:read:" + notifId);
        assertThat(callbacks).contains("qr_details:open:" + qrId);
    }

    @Test
    void renderNotification_unknownReason_usesFallbackEmoji() {
        UUID notifId = UUID.randomUUID();
        UUID qrId = UUID.randomUUID();
        Qr qr = new Qr();
        qr.setId(qrId);
        qr.setName("X");
        qr.setSeqNumber(1L);
        Notification n = new Notification();
        n.setId(notifId);
        n.setQrId(qrId);
        n.setQr(qr);
        n.setReasonId(9999L); // unknown
        n.setText("something");
        n.setCreatedDate(LocalDateTime.now());

        when(notificationService.findByIdOrThrowNotFound(notifId)).thenReturn(n);
        when(notificationService.shouldShowMarkAsReadButton(notifId)).thenReturn(true);

        SceneOutput out = scene.renderNotification(textMessage(notifId, "ignored"));

        assertThat(out.text()).startsWith("🚗");
    }

    @Test
    void renderNotification_alreadyRead_onlyToQrButton() {
        UUID notifId = UUID.randomUUID();
        UUID qrId = UUID.randomUUID();
        Qr qr = new Qr();
        qr.setId(qrId);
        qr.setName("X");
        qr.setSeqNumber(1L);
        Notification n = new Notification();
        n.setId(notifId);
        n.setQrId(qrId);
        n.setQr(qr);
        n.setReasonId(1L);
        n.setText("t");
        n.setCreatedDate(LocalDateTime.now());

        when(notificationService.findByIdOrThrowNotFound(notifId)).thenReturn(n);
        when(notificationService.shouldShowMarkAsReadButton(notifId)).thenReturn(false);

        SceneOutput out = scene.renderNotification(textMessage(notifId, "ignored"));

        String callbacks = out.inlineKeyboard().getKeyboard().stream()
            .flatMap(List::stream)
            .map(b -> b.getCallbackData())
            .reduce("", (a, b) -> a + "|" + b);
        assertThat(callbacks).doesNotContain("notif:read");
        assertThat(callbacks).contains("qr_details:open:" + qrId);
    }

    @Test
    void renderNotification_missingQr_usesFallbackName() {
        UUID notifId = UUID.randomUUID();
        Notification n = new Notification();
        n.setId(notifId);
        n.setReasonId(1L);
        n.setText("t");
        n.setCreatedDate(LocalDateTime.now());

        when(notificationService.findByIdOrThrowNotFound(notifId)).thenReturn(n);
        when(notificationService.shouldShowMarkAsReadButton(notifId)).thenReturn(true);

        SceneOutput out = scene.renderNotification(textMessage(notifId, "ignored"));

        assertThat(out.text()).contains("Метка");
        String callbacks = out.inlineKeyboard().getKeyboard().stream()
            .flatMap(List::stream)
            .map(b -> b.getCallbackData())
            .reduce("", (a, b) -> a + "|" + b);
        assertThat(callbacks).doesNotContain("qr_details:open");
    }

    @Test
    void renderNotification_whenServiceThrows_fallsBackToPlainText() {
        UUID notifId = UUID.randomUUID();
        when(notificationService.findByIdOrThrowNotFound(notifId))
            .thenThrow(new RuntimeException("db down"));
        when(notificationService.shouldShowMarkAsReadButton(notifId)).thenReturn(true);

        SceneOutput out = scene.renderNotification(textMessage(notifId, "Fallback text"));

        assertThat(out.text()).isEqualTo("Fallback text");
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
