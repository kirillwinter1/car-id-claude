package ru.car.service.message.telegram.scene.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.car.dto.FeedbackDto;
import ru.car.enums.FeedbackChannels;
import ru.car.service.FeedbackFacade;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.CallbackData;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.TelegramScene;
import ru.car.service.message.telegram.scene.state.SceneStateRegistry;

import java.util.List;

@Slf4j
@Component
public class SupportScene implements TelegramScene {

    public static final String KEY = "support";
    public static final String ACTION_START = "start";

    private final FeedbackFacade feedbackFacade;
    private final SceneStateRegistry stateRegistry;
    private final TelegramMessages messages;

    public SupportScene(@Lazy FeedbackFacade feedbackFacade,
                        SceneStateRegistry stateRegistry,
                        TelegramMessages messages) {
        this.feedbackFacade = feedbackFacade;
        this.stateRegistry = stateRegistry;
        this.messages = messages;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public SceneOutput handle(CallbackData data, TelegramUpdateContext ctx) {
        if (!ACTION_START.equals(data.action())) return SceneOutput.noop();
        stateRegistry.put(ctx.chatId(), KEY, "text", List.of());
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(List.of(List.of(
            btn(messages.get("tg.common.back"), "support:back"),
            btn(messages.get("tg.common.home"), "home:open")
        )));
        String body = messages.get("tg.support.title") + "\n\n" + messages.get("tg.support.prompt");
        return SceneOutput.editHtml(body, kb);
    }

    @Override
    public SceneOutput handleText(String text, TelegramUpdateContext ctx, List<String> args) {
        String phone = ctx.user() != null && ctx.user().getPhoneNumber() != null
            ? ctx.user().getPhoneNumber() : "unknown";
        FeedbackDto dto = FeedbackDto.builder()
            .email(phone + "@telegram")
            .text(text)
            .channel(FeedbackChannels.TELEGRAM)
            .build();
        try {
            feedbackFacade.send(dto);
        } catch (Exception e) {
            log.error("feedback send failed", e);
        }
        stateRegistry.clear(ctx.chatId());
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(List.of(List.of(
            btn(messages.get("tg.common.home"), "home:open")
        )));
        String body = messages.get("tg.support.sent.title") + "\n\n" + messages.get("tg.support.sent.body");
        return SceneOutput.sendHtml(body, kb);
    }

    private static InlineKeyboardButton btn(String text, String callback) {
        return InlineKeyboardButton.builder().text(text).callbackData(callback).build();
    }
}
