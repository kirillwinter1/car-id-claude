package ru.car.service.message.telegram.render;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.transport.TelegramTransport;

import java.util.Objects;

@Component
public class TelegramRenderer {

    private final TelegramTransport transport;

    public TelegramRenderer(TelegramTransport transport) {
        this.transport = transport;
    }

    public void dispatch(SceneOutput output, long chatId, Message editTargetOrNull) {
        if (output == null || (output.text() == null && output.inlineKeyboard() == null && output.replyKeyboard() == null)) {
            return;
        }
        if (output.editInPlace() && editTargetOrNull != null) {
            dispatchEdit(output, editTargetOrNull);
        } else {
            dispatchSend(output, chatId);
        }
    }

    private void dispatchSend(SceneOutput output, long chatId) {
        SendMessage.SendMessageBuilder builder = SendMessage.builder()
                .chatId(chatId)
                .text(Objects.requireNonNullElse(output.text(), ""));
        if (output.parseMode() != null) {
            builder.parseMode(output.parseMode());
        }
        if (output.inlineKeyboard() != null) {
            builder.replyMarkup(output.inlineKeyboard());
        } else if (output.replyKeyboard() != null) {
            builder.replyMarkup(output.replyKeyboard());
        }
        transport.send(builder.build());
    }

    private void dispatchEdit(SceneOutput output, Message target) {
        if (output.text() != null) {
            EditMessageText.EditMessageTextBuilder builder = EditMessageText.builder()
                    .chatId(target.getChatId())
                    .messageId(target.getMessageId())
                    .text(output.text());
            if (output.parseMode() != null) {
                builder.parseMode(output.parseMode());
            }
            if (output.inlineKeyboard() != null) {
                builder.replyMarkup(output.inlineKeyboard());
            }
            transport.edit(builder.build());
        } else {
            EditMessageReplyMarkup markupEdit = EditMessageReplyMarkup.builder()
                    .chatId(target.getChatId())
                    .messageId(target.getMessageId())
                    .replyMarkup(output.inlineKeyboard())
                    .build();
            transport.edit(markupEdit);
        }
    }
}
