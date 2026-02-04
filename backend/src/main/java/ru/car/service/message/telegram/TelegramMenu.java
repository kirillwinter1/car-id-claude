package ru.car.service.message.telegram;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TelegramMenu {

    public static final String START_CMD = "/start";
    public static final String CONTACT_CMD = "поделиться контактом";
    public static final String AUTH_CMD = "/auth";
    public static final String USER_CMD = "/profile";
    public static final String QRS_CMD = "QR-коды";
    public static final String TEMPORARY_QR_CMD = "Временный QR";
    public static final String QR_CMD = "/qr/";
    public static final String NOTIFICATION_CMD = "/notification/";
    public static final String REGISTER_USER_CMD = "/registerUserCount";
    public static final String ACTIVATE_QR_CMD = "/activateQrCount";
    public static final String SEND_NOTIFICATION_CMD = "/sendNotificationCount";
    public static final String READ_NOTIFICATION_CMD = "/readNotificationCount";

    public static ReplyKeyboardMarkup init() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true); //подгоняем размер
        replyKeyboardMarkup.setOneTimeKeyboard(true); //скрываем после использования

        ArrayList<KeyboardRow> keyboardRows = new ArrayList<>();
        replyKeyboardMarkup.setKeyboard(keyboardRows);

        keyboardRows.add(new KeyboardRow(List.of(new KeyboardButton(TEMPORARY_QR_CMD))));
        keyboardRows.add(new KeyboardRow(List.of(new KeyboardButton(QRS_CMD))));
        return replyKeyboardMarkup;
    }

    public static ReplyKeyboardMarkup admin() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true); //подгоняем размер
        replyKeyboardMarkup.setOneTimeKeyboard(true); //скрываем после использования

        ArrayList<KeyboardRow> keyboardRows = new ArrayList<>();
        replyKeyboardMarkup.setKeyboard(keyboardRows);

        keyboardRows.add(new KeyboardRow(List.of(new KeyboardButton(REGISTER_USER_CMD))));
        keyboardRows.add(new KeyboardRow(List.of(new KeyboardButton(ACTIVATE_QR_CMD))));
        keyboardRows.add(new KeyboardRow(List.of(new KeyboardButton(SEND_NOTIFICATION_CMD))));
        keyboardRows.add(new KeyboardRow(List.of(new KeyboardButton(READ_NOTIFICATION_CMD))));
        keyboardRows.add(new KeyboardRow(List.of(new KeyboardButton(QRS_CMD))));
        keyboardRows.add(new KeyboardRow(List.of(new KeyboardButton(TEMPORARY_QR_CMD))));

        return replyKeyboardMarkup;
    }

    public static ReplyKeyboardMarkup contact() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true); //подгоняем размер
        replyKeyboardMarkup.setOneTimeKeyboard(false); //скрываем после использования

        ArrayList<KeyboardRow> keyboardRows = new ArrayList<>();
        replyKeyboardMarkup.setKeyboard(keyboardRows);

        KeyboardButton button = new KeyboardButton(CONTACT_CMD);
        button.setRequestContact(true);

        keyboardRows.add(new KeyboardRow(List.of(button)));
        return replyKeyboardMarkup;
    }

    public static InlineKeyboardMarkup inline(List<Pair<String, String>> names) {
        if (CollectionUtils.isEmpty(names)) {
            return null;
        }
        List<List<InlineKeyboardButton>> buttons = names.stream()
                .map(name -> List.of(InlineKeyboardButton.builder()
                        .text(name.getKey())
                        .callbackData(name.getValue())
                        .build()))
                .collect(Collectors.toList());

        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(buttons);
        return markupKeyboard;
    }
}
