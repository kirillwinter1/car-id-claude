package ru.car.service.message.telegram.scene.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.car.model.User;
import ru.car.repository.NotificationSettingRepository;
import ru.car.service.UserService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.TelegramScene;
import ru.car.util.MessageUtils;

import java.util.List;

@Slf4j
@Component
public class ProfileScene implements TelegramScene {

    public static final String KEY = "profile";
    public static final String ACTION_OPEN = "open";
    public static final String ACTION_LOGOUT = "logout";
    public static final String ACTION_LOGOUT_CONFIRM = "logout_confirm";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_DELETE_CONFIRM = "delete_confirm";

    private final UserService userService;
    private final NotificationSettingRepository settingRepository;
    private final TelegramMessages messages;

    public ProfileScene(UserService userService, NotificationSettingRepository settingRepository, TelegramMessages messages) {
        this.userService = userService;
        this.settingRepository = settingRepository;
        this.messages = messages;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public SceneOutput handle(CallbackData data, TelegramUpdateContext ctx) {
        return switch (data.action()) {
            case ACTION_OPEN -> render(ctx.user());
            case ACTION_LOGOUT -> renderConfirm(messages.get("tg.profile.confirm.logout"),
                "profile:logout_confirm", "profile:open");
            case ACTION_LOGOUT_CONFIRM -> {
                settingRepository.clearTelegramLink(ctx.userId());
                yield SceneOutput.editHtml(messages.get("tg.profile.logout.done"), null);
            }
            case ACTION_DELETE -> renderConfirm(messages.get("tg.profile.confirm.delete"),
                "profile:delete_confirm", "profile:open");
            case ACTION_DELETE_CONFIRM -> {
                try {
                    userService.deleteUser(ctx.userId());
                } catch (Exception e) {
                    log.error("deleteUser failed for {}", ctx.userId(), e);
                }
                yield SceneOutput.editHtml(messages.get("tg.profile.delete.done"), null);
            }
            default -> SceneOutput.noop();
        };
    }

    private SceneOutput render(User user) {
        String phone = user != null ? MessageUtils.formatPhone(user.getPhoneNumber()) : "-";
        String role = user != null && user.isAdmin()
            ? messages.get("tg.profile.role_admin")
            : messages.get("tg.profile.role_user");
        String body = messages.get("tg.profile.title") + "\n\n" +
            messages.get("tg.profile.body", phone, role);

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(List.of(
            List.of(btn(messages.get("tg.profile.btn.logout"), "profile:logout")),
            List.of(btn(messages.get("tg.profile.btn.delete"), "profile:delete")),
            List.of(
                btn(messages.get("tg.common.back"), "profile:back"),
                btn(messages.get("tg.common.home"), "home:open")
            )
        ));
        return SceneOutput.editHtml(body, kb);
    }

    private SceneOutput renderConfirm(String text, String yesCallback, String noCallback) {
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(List.of(
            List.of(btn(messages.get("tg.common.confirm.yes"), yesCallback)),
            List.of(btn(messages.get("tg.common.confirm.cancel"), noCallback))
        ));
        return SceneOutput.editHtml(text, kb);
    }

    private static InlineKeyboardButton btn(String text, String callback) {
        return InlineKeyboardButton.builder().text(text).callbackData(callback).build();
    }
}
