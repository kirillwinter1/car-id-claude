package ru.car.service.message.telegram.scene.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.car.dto.NotificationDto;
import ru.car.model.Notification;
import ru.car.model.Qr;
import ru.car.model.ReasonDictionary;
import ru.car.service.NotificationFacade;
import ru.car.service.NotificationService;
import ru.car.service.message.TextMessage;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.TelegramScene;
import ru.car.util.MessageUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Component
public class NotificationMarkReadScene implements TelegramScene {

    public static final String KEY = "notif";
    public static final String ACTION_READ = "read";

    private static final Locale RU = new Locale("ru", "RU");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMMM, HH:mm", RU);

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
     * Рендер входящего уведомления — HTML-карточка с эмодзи-по-reason,
     * именем метки, временем и кнопками «отметить прочитанным» / «к метке».
     * Вызывается из Sender-контракта (TelegramBotService.sendNotification).
     */
    public SceneOutput renderNotification(TextMessage message) {
        UUID notifId = message.getNotificationId();
        Notification n;
        try {
            n = notificationService.findByIdOrThrowNotFound(notifId);
        } catch (Exception e) {
            log.warn("Cannot load notification {} for card render, falling back to plain text", notifId, e);
            return SceneOutput.sendWithInline(message.getText(), buildKeyboard(notifId, null));
        }

        String body = buildBody(n);
        InlineKeyboardMarkup kb = buildKeyboard(notifId, n.getQrId());
        return SceneOutput.sendHtml(body, kb);
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

    private String buildBody(Notification n) {
        Qr qr = n.getQr();
        String qrName = MessageUtils.escapeHtml(
            qr != null && qr.getName() != null ? qr.getName() : messages.get("tg.notif.card.fallback_qr_name"));
        String qrMeta = qr != null && qr.getSeqNumber() != null ? " · №" + qr.getSeqNumber() : "";
        String emoji = resolveEmoji(n.getReasonId());

        StringBuilder sb = new StringBuilder();
        sb.append(emoji).append(" <b>").append(qrName).append("</b>").append(qrMeta).append('\n');

        ReasonDictionary reason = n.getReason();
        String reasonDesc = reason != null && reason.getDescription() != null ? reason.getDescription() : null;
        if (reasonDesc != null) {
            sb.append(MessageUtils.escapeHtml(reasonDesc)).append('\n');
        }

        String userText = n.getText();
        if (userText != null && !userText.isBlank() && !userText.equals(reasonDesc)) {
            sb.append('\n').append('«').append(MessageUtils.escapeHtml(userText)).append('»').append('\n');
        }

        LocalDateTime createdDate = n.getCreatedDate();
        if (createdDate != null) {
            sb.append('\n').append("<i>").append(DATE_FMT.format(createdDate)).append("</i>");
        }

        return sb.toString();
    }

    private InlineKeyboardMarkup buildKeyboard(UUID notifId, UUID qrId) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        if (notifId != null && notificationService.shouldShowMarkAsReadButton(notifId)) {
            row.add(btn(messages.get("tg.notif.card.mark_read"),
                new CallbackData(KEY, ACTION_READ, List.of(notifId.toString())).serialize()));
        }
        if (qrId != null) {
            row.add(btn(messages.get("tg.notif.card.to_qr"), "qr_details:open:" + qrId));
        }
        if (row.isEmpty()) return null;
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(List.of(row));
        return kb;
    }

    private String resolveEmoji(Long reasonId) {
        if (reasonId == null) return messages.get("tg.reason.emoji.default");
        try {
            return messages.get("tg.reason.emoji." + reasonId);
        } catch (NoSuchMessageException e) {
            return messages.get("tg.reason.emoji.default");
        }
    }

    private static InlineKeyboardButton btn(String text, String callback) {
        return InlineKeyboardButton.builder().text(text).callbackData(callback).build();
    }
}
