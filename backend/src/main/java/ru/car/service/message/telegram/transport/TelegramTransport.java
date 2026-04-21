package ru.car.service.message.telegram.transport;

import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

public interface TelegramTransport {
    void send(SendMessage message);
    void edit(EditMessageText message);
    void edit(EditMessageReplyMarkup markup);
    void sendDocument(SendDocument document);
}
