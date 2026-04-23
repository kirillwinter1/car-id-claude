package ru.car.service.message.telegram.scene.impl;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.car.model.NotificationSetting;
import ru.car.repository.NotificationSettingRepository;
import ru.car.service.NotificationSettingService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.TelegramScene;

import java.util.List;

@Component
public class NotificationSettingsScene implements TelegramScene {

    public static final String KEY = "settings";
    public static final String ACTION_OPEN = "open";
    public static final String ACTION_TOGGLE = "toggle";

    private final NotificationSettingService service;
    private final NotificationSettingRepository repository;
    private final TelegramMessages messages;

    public NotificationSettingsScene(NotificationSettingService service,
                                     NotificationSettingRepository repository,
                                     TelegramMessages messages) {
        this.service = service;
        this.repository = repository;
        this.messages = messages;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public SceneOutput handle(CallbackData data, TelegramUpdateContext ctx) {
        return switch (data.action()) {
            case ACTION_OPEN -> render(ctx.userId());
            case ACTION_TOGGLE -> {
                if (data.args().isEmpty()) yield SceneOutput.noop();
                service.toggleChannel(ctx.userId(), data.args().get(0));
                yield render(ctx.userId());
            }
            default -> SceneOutput.noop();
        };
    }

    private SceneOutput render(Long userId) {
        NotificationSetting s = repository.findByUserId(userId);
        String body = messages.get("tg.settings.title") + "\n\n" +
            messages.get("tg.settings.body",
                formatState(s.getPushEnabled()),
                formatState(s.getCallEnabled()),
                formatState(s.getTelegramEnabled()));

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(List.of(
            List.of(btn(pushLabel(s.getPushEnabled()), "settings:toggle:push")),
            List.of(btn(callLabel(s.getCallEnabled()), "settings:toggle:call")),
            List.of(btn(telegramLabel(s.getTelegramEnabled()), "settings:toggle:telegram")),
            List.of(
                btn(messages.get("tg.common.back"), "settings:back"),
                btn(messages.get("tg.common.home"), "home:open")
            )
        ));
        return SceneOutput.editHtml(body, kb);
    }

    private String formatState(Boolean on) {
        return Boolean.TRUE.equals(on) ? messages.get("tg.settings.on") : messages.get("tg.settings.off");
    }

    private String pushLabel(Boolean on) {
        return Boolean.TRUE.equals(on) ? messages.get("tg.settings.btn.toggle_push_on") : messages.get("tg.settings.btn.toggle_push_off");
    }

    private String callLabel(Boolean on) {
        return Boolean.TRUE.equals(on) ? messages.get("tg.settings.btn.toggle_call_on") : messages.get("tg.settings.btn.toggle_call_off");
    }

    private String telegramLabel(Boolean on) {
        return Boolean.TRUE.equals(on) ? messages.get("tg.settings.btn.toggle_telegram_on") : messages.get("tg.settings.btn.toggle_telegram_off");
    }

    private static InlineKeyboardButton btn(String text, String callback) {
        return InlineKeyboardButton.builder().text(text).callbackData(callback).build();
    }
}
