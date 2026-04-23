package ru.car.service.message.telegram.scene.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.car.dto.PageParam;
import ru.car.enums.NotificationStatus;
import ru.car.model.Notification;
import ru.car.model.NotificationSetting;
import ru.car.service.NotificationService;
import ru.car.service.message.TextMessage;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.TelegramScene;
import ru.car.util.MessageUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class NotificationListScene implements TelegramScene {

    public static final String KEY = "notif_list";
    public static final String ACTION_OPEN = "open";
    public static final String ACTION_VIEW = "view";
    private static final int PAGE_SIZE = 5;

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM");

    private final NotificationService notificationService;
    private final NotificationMarkReadScene markReadScene;
    private final TelegramMessages messages;

    public NotificationListScene(NotificationService notificationService,
                                 NotificationMarkReadScene markReadScene,
                                 TelegramMessages messages) {
        this.notificationService = notificationService;
        this.markReadScene = markReadScene;
        this.messages = messages;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public SceneOutput handle(CallbackData data, TelegramUpdateContext ctx) {
        return switch (data.action()) {
            case ACTION_OPEN -> renderList(data.args(), ctx);
            case ACTION_VIEW -> viewNotification(data.args(), ctx);
            default -> SceneOutput.noop();
        };
    }

    private SceneOutput renderList(List<String> args, TelegramUpdateContext ctx) {
        String tab = !args.isEmpty() ? args.get(0) : "all";
        int page = args.size() > 1 ? parseIntSafe(args.get(1), 1) : 1;
        UUID qrFilter = null;
        if (args.size() >= 4 && "qr".equals(args.get(2)) && MessageUtils.isUUID(args.get(3))) {
            qrFilter = UUID.fromString(args.get(3));
        }

        PageParam pageParam = PageParam.builder().page(page - 1).size(PAGE_SIZE).build();
        List<Notification> notifications = notificationService.findForBot(ctx.userId(), tab, qrFilter, pageParam);

        String tailFilter = qrFilter != null ? ":qr:" + qrFilter : "";

        if (notifications.isEmpty() && page == 1) {
            InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
            kb.setKeyboard(List.of(List.of(
                btn(messages.get("tg.common.back"), KEY + ":back"),
                btn(messages.get("tg.common.home"), "home:open")
            )));
            return SceneOutput.editHtml(messages.get("tg.notif.list.empty"), kb);
        }

        StringBuilder body = new StringBuilder(messages.get("tg.notif.list.title")).append("\n\n");
        body.append("all".equals(tab) ? messages.get("tg.notif.list.tabs_all") : messages.get("tg.notif.list.tabs_unread"));
        body.append("\n\n");
        for (Notification n : notifications) {
            body.append(formatItem(n)).append("\n\n");
        }

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Notification n : notifications) {
            rows.add(List.of(btn(formatButton(n), KEY + ":view:" + n.getId())));
        }
        rows.add(List.of(
            btn(messages.get("tg.notif.list.btn.tab_all"), KEY + ":open:all:1" + tailFilter),
            btn(messages.get("tg.notif.list.btn.tab_unread"), KEY + ":open:unread:1" + tailFilter)
        ));
        if (notifications.size() == PAGE_SIZE || page > 1) {
            List<InlineKeyboardButton> pagination = new ArrayList<>();
            if (page > 1) {
                pagination.add(btn("‹", KEY + ":open:" + tab + ":" + (page - 1) + tailFilter));
            }
            pagination.add(btn(messages.get("tg.notif.list.pagination", page, "?"), "noop:noop"));
            if (notifications.size() == PAGE_SIZE) {
                pagination.add(btn("›", KEY + ":open:" + tab + ":" + (page + 1) + tailFilter));
            }
            rows.add(pagination);
        }
        rows.add(List.of(
            btn(messages.get("tg.common.back"), KEY + ":back"),
            btn(messages.get("tg.common.home"), "home:open")
        ));

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(rows);
        return SceneOutput.editHtml(body.toString(), kb);
    }

    private SceneOutput viewNotification(List<String> args, TelegramUpdateContext ctx) {
        if (args.isEmpty() || !MessageUtils.isUUID(args.get(0))) {
            return SceneOutput.noop();
        }
        UUID id = UUID.fromString(args.get(0));
        Notification n = notificationService.findByIdOrThrowNotFound(id);
        NotificationSetting setting = new NotificationSetting();
        setting.setTelegramDialogId(ctx.chatId());
        TextMessage msg = TextMessage.builder()
            .setting(setting)
            .notificationId(n.getId())
            .text(n.getText() != null ? n.getText() : "")
            .build();
        return markReadScene.renderNotification(msg);
    }

    private String formatItem(Notification n) {
        String emoji = NotificationStatus.UNREAD.equals(n.getStatus()) ? "🔴" : "⚪";
        String when = formatWhen(n.getCreatedDate());
        String qrName = "метка";
        String text = MessageUtils.escapeHtml(n.getText() != null ? n.getText() : "");
        return emoji + " " + when + " · <b>" + qrName + "</b>\n   " + text;
    }

    private String formatButton(Notification n) {
        String emoji = NotificationStatus.UNREAD.equals(n.getStatus()) ? "🔴" : "⚪";
        return emoji + " " + formatWhen(n.getCreatedDate());
    }

    private String formatWhen(LocalDateTime dt) {
        if (dt == null) return "";
        LocalDate today = LocalDate.now();
        LocalDate d = dt.toLocalDate();
        if (d.equals(today)) return TIME.format(dt);
        if (d.equals(today.minusDays(1))) return "вчера, " + TIME.format(dt);
        return DATE.format(dt);
    }

    private int parseIntSafe(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return fallback; }
    }

    private static InlineKeyboardButton btn(String text, String callback) {
        return InlineKeyboardButton.builder().text(text).callbackData(callback).build();
    }
}
