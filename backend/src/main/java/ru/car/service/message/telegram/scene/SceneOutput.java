package ru.car.service.message.telegram.scene;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

public record SceneOutput(
        String text,
        InlineKeyboardMarkup inlineKeyboard,
        ReplyKeyboard replyKeyboard,
        boolean editInPlace,
        String parseMode
) {
    public static SceneOutput send(String text, ReplyKeyboard replyKeyboard) {
        return new SceneOutput(text, null, replyKeyboard, false, null);
    }

    public static SceneOutput sendWithInline(String text, InlineKeyboardMarkup inline) {
        return new SceneOutput(text, inline, null, false, null);
    }

    public static SceneOutput editText(String text, InlineKeyboardMarkup inline) {
        return new SceneOutput(text, inline, null, true, null);
    }

    public static SceneOutput editMarkup(InlineKeyboardMarkup inline) {
        return new SceneOutput(null, inline, null, true, null);
    }

    public static SceneOutput noop() {
        return new SceneOutput(null, null, null, false, null);
    }
}
