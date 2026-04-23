package ru.car.service.message.telegram.scene;

import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

public record SceneOutput(
        String text,
        InlineKeyboardMarkup inlineKeyboard,
        ReplyKeyboard replyKeyboard,
        boolean editInPlace,
        String parseMode,
        InputFile photo,
        String caption
) {
    public static SceneOutput send(String text, ReplyKeyboard replyKeyboard) {
        return new SceneOutput(text, null, replyKeyboard, false, null, null, null);
    }

    public static SceneOutput sendWithInline(String text, InlineKeyboardMarkup inline) {
        return new SceneOutput(text, inline, null, false, null, null, null);
    }

    public static SceneOutput sendHtml(String text, InlineKeyboardMarkup inline) {
        return new SceneOutput(text, inline, null, false, "HTML", null, null);
    }

    public static SceneOutput editText(String text, InlineKeyboardMarkup inline) {
        return new SceneOutput(text, inline, null, true, null, null, null);
    }

    public static SceneOutput editHtml(String text, InlineKeyboardMarkup inline) {
        return new SceneOutput(text, inline, null, true, "HTML", null, null);
    }

    public static SceneOutput editMarkup(InlineKeyboardMarkup inline) {
        return new SceneOutput(null, inline, null, true, null, null, null);
    }

    public static SceneOutput photo(InputFile photo, String caption) {
        return new SceneOutput(null, null, null, false, "HTML", photo, caption);
    }

    public static SceneOutput noop() {
        return new SceneOutput(null, null, null, false, null, null, null);
    }
}
