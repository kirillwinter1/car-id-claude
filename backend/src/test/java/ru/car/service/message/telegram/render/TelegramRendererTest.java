package ru.car.service.message.telegram.render;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.transport.TelegramTransport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TelegramRendererTest {

    @Mock TelegramTransport transport;

    private TelegramRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new TelegramRenderer(transport);
    }

    @Test
    void noop_whenOutputIsNull() {
        renderer.dispatch(null, 42L, null);
        verify(transport, never()).send(org.mockito.ArgumentMatchers.any());
        verify(transport, never()).edit(org.mockito.ArgumentMatchers.any(EditMessageText.class));
        verify(transport, never()).edit(org.mockito.ArgumentMatchers.any(EditMessageReplyMarkup.class));
    }

    @Test
    void noop_whenSendOutputHasNoContent() {
        renderer.dispatch(SceneOutput.noop(), 42L, null);
        verify(transport, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sendText_withReplyKeyboard() {
        ReplyKeyboardMarkup reply = new ReplyKeyboardMarkup();
        renderer.dispatch(SceneOutput.send("hello", reply), 42L, null);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(transport).send(captor.capture());
        assertThat(captor.getValue().getText()).isEqualTo("hello");
        assertThat(captor.getValue().getReplyMarkup()).isEqualTo(reply);
    }

    @Test
    void sendText_withInlineKeyboard() {
        InlineKeyboardMarkup inline = new InlineKeyboardMarkup();
        renderer.dispatch(SceneOutput.sendWithInline("hi", inline), 42L, null);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(transport).send(captor.capture());
        assertThat(captor.getValue().getText()).isEqualTo("hi");
        assertThat(captor.getValue().getReplyMarkup()).isEqualTo(inline);
    }

    @Test
    void editText_withInlineKeyboard() {
        InlineKeyboardMarkup inline = new InlineKeyboardMarkup();
        renderer.dispatch(SceneOutput.editText("new text", inline), 42L, editTarget(42L, 99));

        ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
        verify(transport).edit(captor.capture());
        assertThat(captor.getValue().getText()).isEqualTo("new text");
        assertThat(captor.getValue().getMessageId()).isEqualTo(99);
    }

    @Test
    void editMarkup_withNullKeyboard_removesKeyboard() {
        // REGRESSION TEST for C1 — SceneOutput.editMarkup(null) must reach the transport
        renderer.dispatch(SceneOutput.editMarkup(null), 42L, editTarget(42L, 99));

        ArgumentCaptor<EditMessageReplyMarkup> captor = ArgumentCaptor.forClass(EditMessageReplyMarkup.class);
        verify(transport).edit(captor.capture());
        assertThat(captor.getValue().getReplyMarkup()).isNull();  // null → Telegram removes keyboard
        assertThat(captor.getValue().getMessageId()).isEqualTo(99);
    }

    @Test
    void editInPlace_fallsBackToSend_whenTargetIsNull() {
        renderer.dispatch(SceneOutput.editText("x", new InlineKeyboardMarkup()), 42L, null);

        verify(transport).send(org.mockito.ArgumentMatchers.any(SendMessage.class));
        verify(transport, never()).edit(org.mockito.ArgumentMatchers.any(EditMessageText.class));
    }

    @Test
    void dispatchesPhoto_whenSceneOutputHasPhoto() {
        org.telegram.telegrambots.meta.api.objects.InputFile inputFile =
            new org.telegram.telegrambots.meta.api.objects.InputFile(
                new java.io.ByteArrayInputStream(new byte[]{1,2,3}), "qr.png");
        SceneOutput output = SceneOutput.photo(inputFile, "Audi Q5");

        renderer.dispatch(output, 42L, null);

        org.mockito.ArgumentCaptor<org.telegram.telegrambots.meta.api.methods.send.SendPhoto> captor =
            org.mockito.ArgumentCaptor.forClass(org.telegram.telegrambots.meta.api.methods.send.SendPhoto.class);
        verify(transport).sendPhoto(captor.capture());
        assertThat(captor.getValue().getCaption()).isEqualTo("Audi Q5");
        assertThat(captor.getValue().getParseMode()).isEqualTo("HTML");
    }

    private static Message editTarget(long chatId, int messageId) {
        Message msg = new Message();
        org.telegram.telegrambots.meta.api.objects.Chat chat = new org.telegram.telegrambots.meta.api.objects.Chat();
        chat.setId(chatId);
        msg.setChat(chat);
        msg.setMessageId(messageId);
        return msg;
    }
}
