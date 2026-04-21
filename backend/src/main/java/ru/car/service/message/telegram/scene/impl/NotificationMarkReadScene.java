package ru.car.service.message.telegram.scene.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.car.dto.NotificationDto;
import ru.car.service.NotificationFacade;
import ru.car.service.NotificationService;
import ru.car.service.message.TextMessage;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.TelegramScene;
import ru.car.util.MessageUtils;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class NotificationMarkReadScene implements TelegramScene {

    public static final String KEY = "notif";
    public static final String ACTION_READ = "read";

    private final NotificationFacade notificationFacade;
    private final NotificationService notificationService;
    private final TelegramMessages messages;

    public NotificationMarkReadScene(@Lazy NotificationFacade notificationFacade,
                                      NotificationService notificationService,
                                      TelegramMessages messages) {
        this.notificationFacade = notificationFacade;
        this.notificationService = notificationService;
        this.messages = messages;
    }

    @Override
    public String key() {
        return KEY;
    }

    /**
     * Рендер входящего уведомления с кнопкой «отметить прочитанным», если статус ещё не READ.
     * Вызывается из Sender-контракта (TelegramBotService.sendNotification).
     */
    public SceneOutput renderNotification(TextMessage message) {
        UUID id = message.getNotificationId();
        InlineKeyboardMarkup markup = null;
        if (notificationService.shouldShowMarkAsReadButton(id)) {
            markup = buildMarkReadButton(id);
        }
        return SceneOutput.sendWithInline(message.getText(), markup);
    }

    @Override
    public SceneOutput handle(CallbackData data, TelegramUpdateContext ctx) {
        if (!ACTION_READ.equals(data.action()) || data.args().isEmpty()) {
            return SceneOutput.noop();
        }
        String rawUuid = data.args().get(0);
        if (!MessageUtils.isUUID(rawUuid)) {
            return SceneOutput.noop();
        }
        UUID id = UUID.fromString(rawUuid);
        try {
            notificationFacade.readBy(NotificationDto.builder().notificationId(id).build(), ctx.userId());
        } catch (Exception e) {
            log.warn("Failed to mark notification {} as read for user {}", id, ctx.userId(), e);
        }
        return SceneOutput.editMarkup(null);
    }

    private InlineKeyboardMarkup buildMarkReadButton(UUID id) {
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(messages.get("tg.notification.mark_read"))
                .callbackData(new CallbackData(KEY, ACTION_READ, List.of(id.toString())).serialize())
                .build();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(List.of(button)));
        return markup;
    }
}
