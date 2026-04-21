package ru.car.service.message.telegram.router;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.car.model.User;

import java.util.Optional;

public record TelegramUpdateContext(long chatId, Long userId, User user, Update update) {

    public static long extractChatId(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        }
        throw new IllegalArgumentException("Update has neither callback nor message");
    }

    public Optional<Message> callbackMessage() {
        if (update != null && update.hasCallbackQuery()) {
            return Optional.ofNullable(update.getCallbackQuery().getMessage());
        }
        return Optional.empty();
    }

    public Optional<String> text() {
        if (update != null && update.hasMessage() && update.getMessage().hasText()) {
            return Optional.of(update.getMessage().getText());
        }
        return Optional.empty();
    }

    public boolean hasCallback() {
        return update != null && update.hasCallbackQuery();
    }
}
