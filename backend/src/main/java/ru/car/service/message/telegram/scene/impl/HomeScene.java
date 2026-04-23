package ru.car.service.message.telegram.scene.impl;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.car.service.NotificationService;
import ru.car.service.QrService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.TelegramScene;

import java.util.ArrayList;
import java.util.List;

@Component
public class HomeScene implements TelegramScene {

    public static final String KEY = "home";

    private final NotificationService notificationService;
    private final QrService qrService;
    private final TelegramMessages messages;

    public HomeScene(NotificationService notificationService, QrService qrService, TelegramMessages messages) {
        this.notificationService = notificationService;
        this.qrService = qrService;
        this.messages = messages;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public SceneOutput render(TelegramUpdateContext ctx) {
        return SceneOutput.sendHtml(messages.get("tg.home.title"), buildKeyboard(ctx.userId()));
    }

    @Override
    public SceneOutput handle(CallbackData data, TelegramUpdateContext ctx) {
        return SceneOutput.editHtml(messages.get("tg.home.title"), buildKeyboard(ctx.userId()));
    }

    public SceneOutput renderUnknown(TelegramUpdateContext ctx) {
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(List.of(List.of(
            InlineKeyboardButton.builder()
                .text(messages.get("tg.unknown.btn.open_home"))
                .callbackData(new CallbackData("home", "open", List.of()).serialize())
                .build()
        )));
        return SceneOutput.sendHtml(messages.get("tg.home.unknown_command"), kb);
    }

    private InlineKeyboardMarkup buildKeyboard(Long userId) {
        int unread = notificationService.countUnreadByUserId(userId);
        int qrs = qrService.countByUserId(userId);

        String notifBtn = unread > 0
            ? messages.get("tg.home.btn.notifications", unread)
            : messages.get("tg.home.btn.notifications_zero");
        String qrsBtn = qrs > 0
            ? messages.get("tg.home.btn.qrs", qrs)
            : messages.get("tg.home.btn.qrs_zero");

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row(notifBtn, "notif_list:open:all:1"));
        rows.add(row(qrsBtn, "qr_list:open"));
        rows.add(row(messages.get("tg.home.btn.temp_qr"), "temp_qr:create"));
        rows.add(row(messages.get("tg.home.btn.report"), "report:start"));
        rows.add(row(messages.get("tg.home.btn.settings"), "settings:open"));
        rows.add(row(messages.get("tg.home.btn.support"), "support:start"));
        rows.add(row(messages.get("tg.home.btn.marketplace"), "marketplace:open"));
        rows.add(row(messages.get("tg.home.btn.profile"), "profile:open"));

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(rows);
        return kb;
    }

    private static List<InlineKeyboardButton> row(String text, String callback) {
        return List.of(InlineKeyboardButton.builder().text(text).callbackData(callback).build());
    }
}
