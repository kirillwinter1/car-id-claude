package ru.car.service.message.telegram.scene.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import ru.car.dto.PageParam;
import ru.car.enums.NotificationStatus;
import ru.car.model.Notification;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationListSceneTest {

    @Mock NotificationService notificationService;
    @Mock NotificationMarkReadScene markReadScene;

    NotificationListScene scene;

    @BeforeEach
    void setUp() {
        scene = new NotificationListScene(notificationService, markReadScene, messages());
    }

    @Test
    void keyIsNotifList() {
        assertThat(scene.key()).isEqualTo("notif_list");
    }

    @Test
    void handle_openAllPage1_renderList_empty() {
        when(notificationService.findForBot(eq(42L), eq("all"), isNull(), any(PageParam.class)))
            .thenReturn(List.of());

        SceneOutput output = scene.handle(
            new CallbackData("notif_list", "open", List.of("all", "1")),
            new TelegramUpdateContext(100L, 42L, null, null));

        assertThat(output.text()).contains("Уведомления");
        assertThat(output.editInPlace()).isTrue();
    }

    @Test
    void handle_openWithQrFilter_passesQrIdToService() {
        UUID qr = UUID.randomUUID();
        when(notificationService.findForBot(eq(42L), eq("all"), eq(qr), any(PageParam.class)))
            .thenReturn(List.of());

        SceneOutput output = scene.handle(
            new CallbackData("notif_list", "open", List.of("all", "1", "qr", qr.toString())),
            new TelegramUpdateContext(100L, 42L, null, null));

        assertThat(output.editInPlace()).isTrue();
    }

    @Test
    void handle_view_delegatesToMarkReadScene() {
        UUID notifId = UUID.randomUUID();
        Notification n = new Notification();
        n.setId(notifId);
        n.setText("test");
        n.setStatus(NotificationStatus.UNREAD);
        n.setCreatedDate(LocalDateTime.now());
        when(notificationService.findByIdOrThrowNotFound(notifId)).thenReturn(n);
        when(markReadScene.renderNotification(any(TextMessage.class)))
            .thenReturn(SceneOutput.sendWithInline("body", null));

        SceneOutput output = scene.handle(
            new CallbackData("notif_list", "view", List.of(notifId.toString())),
            new TelegramUpdateContext(100L, 42L, null, null));

        assertThat(output.text()).isEqualTo("body");
    }

    private static TelegramMessages messages() {
        var src = new ReloadableResourceBundleMessageSource();
        src.setBasename("classpath:i18n/telegram");
        src.setDefaultEncoding("UTF-8");
        src.setFallbackToSystemLocale(false);
        return new TelegramMessages(src);
    }
}
