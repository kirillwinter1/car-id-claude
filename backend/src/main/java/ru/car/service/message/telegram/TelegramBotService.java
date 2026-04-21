package ru.car.service.message.telegram;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.car.exception.MessageNotSendException;
import ru.car.model.NotificationSetting;
import ru.car.service.message.Sender;
import ru.car.service.message.TextMessage;
import ru.car.service.message.telegram.render.TelegramRenderer;
import ru.car.service.message.telegram.router.TelegramRouter;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.service.message.telegram.scene.impl.NotificationMarkReadScene;
import ru.car.service.message.telegram.transport.TelegramTransport;

import java.util.Objects;

@Slf4j
@Component
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramBotService extends TelegramLongPollingBot implements Sender, TelegramTransport {

    private final TelegramProperties properties;
    private final TelegramRouter router;
    private final NotificationMarkReadScene notificationMarkReadScene;
    private final TelegramRenderer renderer;

    public TelegramBotService(TelegramProperties properties,
                              TelegramRouter router,
                              NotificationMarkReadScene notificationMarkReadScene,
                              TelegramRenderer renderer) {
        super(properties.getToken());
        this.properties = properties;
        this.router = router;
        this.notificationMarkReadScene = notificationMarkReadScene;
        this.renderer = renderer;
    }

    public void init() throws TelegramApiException {
        if (BooleanUtils.isFalse(properties.getEnable())) {
            return;
        }
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(this);
    }

    @Override
    public String getBotUsername() {
        return properties.getBot();
    }

    @Override
    public void onUpdateReceived(Update update) {
        router.route(update);
    }

    // --- Sender ---

    @Override
    public String getServiceName() {
        return "telegram";
    }

    @Override
    public boolean sendNotification(TextMessage message) {
        SceneOutput output = notificationMarkReadScene.renderNotification(message);
        renderer.dispatch(output, message.getSetting().getTelegramDialogId(), null);
        return true;
    }

    @Override
    public boolean canSendNotification(NotificationSetting setting) {
        return setting.getTelegramEnabled() && Objects.nonNull(setting.getTelegramDialogId());
    }

    public boolean sendFeedback(String from, String text) {
        if (Objects.isNull(properties.getFeedbackChannelId())) {
            return false;
        }
        String body = String.format("%s:\n%s", from, text);
        SendMessage message = SendMessage.builder()
                .chatId(properties.getFeedbackChannelId())
                .text(body)
                .build();
        send(message);
        return true;
    }

    // --- TelegramTransport ---

    @Override
    public void send(SendMessage message) {
        executeSafe(message, "send");
    }

    @Override
    public void edit(EditMessageText message) {
        executeSafe(message, "editText");
    }

    @Override
    public void edit(EditMessageReplyMarkup markup) {
        executeSafe(markup, "editMarkup");
    }

    @Override
    public void sendDocument(SendDocument document) {
        try {
            execute(document);
        } catch (TelegramApiException e) {
            log.error("Can't send telegram document", e);
            throw new MessageNotSendException(e);
        }
    }

    private <T extends java.io.Serializable, M extends org.telegram.telegrambots.meta.api.methods.BotApiMethod<T>>
    void executeSafe(M method, String kind) {
        try {
            execute(method);
        } catch (TelegramApiException e) {
            log.error("Can't {} telegram message", kind, e);
            throw new MessageNotSendException(e);
        }
    }
}
