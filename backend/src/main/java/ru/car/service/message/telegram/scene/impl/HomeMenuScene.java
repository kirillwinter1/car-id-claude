package ru.car.service.message.telegram.scene.impl;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.router.TelegramUpdateContext;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.TelegramScene;

import java.util.ArrayList;
import java.util.List;

@Component
public class HomeMenuScene implements TelegramScene {

    public static final String KEY = "home";

    private final TelegramMessages messages;

    public HomeMenuScene(TelegramMessages messages) {
        this.messages = messages;
    }

    @Override
    public String key() {
        return KEY;
    }

    /**
     * Рендер отклика на неизвестную команду: текст «Неизвестная команда» + reply-клавиатура.
     * Вызывается из роутера, не через render()/handle() стандартный поток.
     */
    public SceneOutput renderUnknown(TelegramUpdateContext ctx) {
        return SceneOutput.send(messages.get("tg.home.unknown_command"), buildMainKeyboard());
    }

    /**
     * Публичный метод для других сцен / роутера — получить reply-клавиатуру для финального ответа.
     */
    public ReplyKeyboardMarkup mainKeyboard() {
        return buildMainKeyboard();
    }

    private ReplyKeyboardMarkup buildMainKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(new KeyboardRow(List.of(new KeyboardButton(messages.get("tg.home.btn.temp_qr")))));
        rows.add(new KeyboardRow(List.of(new KeyboardButton(messages.get("tg.home.btn.qrs")))));
        keyboard.setKeyboard(rows);
        return keyboard;
    }
}
