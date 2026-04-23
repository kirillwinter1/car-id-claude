package ru.car.service.message.telegram.router;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.car.model.User;
import ru.car.repository.NotificationSettingRepository;
import ru.car.service.UserService;
import ru.car.service.message.telegram.auth.TelegramAuthorizationService;
import ru.car.service.message.telegram.render.TelegramRenderer;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.SceneRegistry;
import ru.car.service.message.telegram.scene.TelegramScene;
import ru.car.service.message.telegram.scene.impl.HomeMenuScene;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class TelegramRouter {

    private final NotificationSettingRepository settingRepository;
    private final UserService userService;
    private final SceneRegistry sceneRegistry;
    private final TelegramAuthorizationService authService;
    private final HomeMenuScene homeMenuScene;
    private final TelegramRenderer renderer;

    public TelegramRouter(NotificationSettingRepository settingRepository,
                          UserService userService,
                          SceneRegistry sceneRegistry,
                          TelegramAuthorizationService authService,
                          HomeMenuScene homeMenuScene,
                          TelegramRenderer renderer) {
        this.settingRepository = settingRepository;
        this.userService = userService;
        this.sceneRegistry = sceneRegistry;
        this.authService = authService;
        this.homeMenuScene = homeMenuScene;
        this.renderer = renderer;
    }

    @Transactional
    public void route(Update update) {
        if (!update.hasCallbackQuery() && !update.hasMessage()) {
            return;
        }
        long chatId = TelegramUpdateContext.extractChatId(update);

        if (!settingRepository.existsByTelegramDialogId(chatId)) {
            String text = extractAuthText(update);
            SceneOutput output = authService.handle(chatId, text);
            renderer.dispatch(output, chatId, null);
            if (settingRepository.existsByTelegramDialogId(chatId)) {
                Long newUserId = settingRepository.findUserIdByTelegramDialogId(chatId);
                User newUser = userService.getUserOrThrowNotFound(newUserId);
                TelegramUpdateContext homeCtx = new TelegramUpdateContext(chatId, newUserId, newUser, update);
                sceneRegistry.findByKey("home").ifPresent(home -> {
                    SceneOutput homeOutput = home.render(homeCtx);
                    renderer.dispatch(homeOutput, chatId, null);
                });
            }
            return;
        }

        Long userId = settingRepository.findUserIdByTelegramDialogId(chatId);
        User user = userService.getUserOrThrowNotFound(userId);
        TelegramUpdateContext ctx = new TelegramUpdateContext(chatId, userId, user, update);

        if (update.hasCallbackQuery()) {
            handleCallback(ctx);
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            handleText(ctx);
        }
    }

    private void handleCallback(TelegramUpdateContext ctx) {
        String raw = ctx.update().getCallbackQuery().getData();
        Optional<CallbackData> parsed = CallbackData.parse(raw);
        if (parsed.isEmpty()) {
            dispatchUnknown(ctx);
            return;
        }
        CallbackData data = parsed.get();
        Optional<TelegramScene> sceneOpt = sceneRegistry.findByKey(data.scene());
        if (sceneOpt.isEmpty()) {
            dispatchUnknown(ctx);
            return;
        }
        TelegramScene scene = sceneOpt.get();

        if ("back".equals(data.action())) {
            Optional<TelegramScene> parent = sceneRegistry.findByKey(scene.parentKey());
            if (parent.isPresent()) {
                SceneOutput parentOutput = parent.get().render(ctx);
                // force edit-in-place for back navigation even if parent's render returned a fresh send
                SceneOutput editVersion = new SceneOutput(
                        parentOutput.text(), parentOutput.inlineKeyboard(), null, true,
                        parentOutput.parseMode(), null, null);
                renderer.dispatch(editVersion, ctx.chatId(), ctx.callbackMessage().orElse(null));
                return;
            }
        }

        SceneOutput output = scene.handle(data, ctx);
        Message editTarget = ctx.callbackMessage().orElse(null);
        renderer.dispatch(output, ctx.chatId(), editTarget);
    }

    private void handleText(TelegramUpdateContext ctx) {
        String text = ctx.text().orElse("");
        Optional<TelegramScene> scene = sceneRegistry.findByText(text);
        if (scene.isEmpty()) {
            dispatchUnknown(ctx);
            return;
        }
        SceneOutput output = scene.get().render(ctx);
        renderer.dispatch(output, ctx.chatId(), null);
    }

    private void dispatchUnknown(TelegramUpdateContext ctx) {
        SceneOutput output = homeMenuScene.renderUnknown(ctx);
        renderer.dispatch(output, ctx.chatId(), null);
    }

    private static String extractAuthText(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.getContact() != null && message.getContact().getPhoneNumber() != null) {
                return message.getContact().getPhoneNumber();
            }
            return Objects.requireNonNullElse(message.getText(), "");
        }
        return "";
    }
}
