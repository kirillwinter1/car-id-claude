package ru.car.service.message.telegram.router;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.car.model.User;

public record TelegramUpdateContext(long chatId, Long userId, User user, Update update) {
}
