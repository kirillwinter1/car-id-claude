package ru.car.service.message.telegram;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.car.constants.ApplicationConstants;
import ru.car.enums.BatchTemplates;
import ru.car.enums.NotificationStatus;
import ru.car.exception.MessageNotSendException;
import ru.car.model.NotificationSetting;
import ru.car.model.Qr;
import ru.car.repository.NotificationRepository;
import ru.car.service.message.Sender;
import ru.car.service.message.TextMessage;
import ru.car.util.SvgUtils;
import ru.car.util.svg.PdfConverter;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Setter
@Component
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramBotService extends TelegramLongPollingBot implements Sender {
    private final TelegramProperties properties;
    private final NotificationRepository notificationRepository;
    private TelegramLogicService telegramLogicService;
    private ReplyKeyboardMarkup menu = TelegramMenu.init();
    private ReplyKeyboardMarkup adminMenu = TelegramMenu.admin();

    public TelegramBotService(TelegramProperties properties, NotificationRepository notificationRepository) {
        super(properties.getToken());
        this.properties = properties;
        this.notificationRepository = notificationRepository;
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
        long chatId;
        if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
        } else if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
        } else {
            return;
        }
        telegramLogicService.onUpdateReceived(chatId, update);
    }

    public boolean sendQrs(Long chatId, List<Qr> qrs, boolean isAdmin) {
        if (CollectionUtils.isEmpty(qrs)) {
            return sendMessage(chatId, ApplicationConstants.USER_HAS_NO_QR, isAdmin);
        } else {
            return sendMessage(chatId, "QR-коды:", getQrs(qrs));
        }
    }

    private InlineKeyboardMarkup getQrs(List<Qr> qrs) {
        List<Pair<String, String>> namesLink = CollectionUtils.emptyIfNull(qrs).stream()
                .map(qr -> Pair.of(qr.getName() + " " + qr.getSeqNumber(), TelegramMenu.QR_CMD + qr.getId()))
                .collect(Collectors.toList());
        return TelegramMenu.inline(namesLink);
    }

    private InlineKeyboardMarkup readNotification(UUID id) {
        NotificationStatus status = notificationRepository.findById(id)
                .map(n -> n.getStatus())
                .orElse(null);
        if (NotificationStatus.READ.equals(status)) {
            return null;
        }
        Pair<String, String> namesLink = Pair.of("отметить прочитанным", TelegramMenu.NOTIFICATION_CMD + id);
        return TelegramMenu.inline(List.of(namesLink));
    }

    public void editMarkup(Message message, List<Qr> qrs) {
        EditMessageReplyMarkup edit = EditMessageReplyMarkup.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .replyMarkup(getQrs(qrs))
                .build();
        try {
            execute(edit);
        } catch (TelegramApiException e) {
            log.error("Can't send telegram message", e);
        }
    }

    public void sendQr(Long chatId, UUID id, BatchTemplates template) {
        try {
            SendDocument message = SendDocument.builder()
                    .chatId(chatId)
                    .document(new InputFile(new ByteArrayInputStream(PdfConverter.create(SvgUtils.getSvgText(id.toString(), template))),
                            "qr.pdf"))
                    .build();
            execute(message);
            log.info("Отправил \"qr.pdf\" {}", id);
        } catch (TelegramApiException ex) {
            log.error("Ошибка при отправке \"qr.pdf\" {}\n{}", id, ex.getMessage());
            ex.printStackTrace();
        } catch (Exception e) {
            throw new MessageNotSendException(e);
        }
    }

    private boolean sendNotification(Long chatId, String textToSend, UUID notificationId) {
        return sendMessage(chatId, textToSend, readNotification(notificationId));
    }

    public boolean sendMessage(Long chatId, String textToSend, boolean isAdmin) {
        return sendMessage(chatId, textToSend, isAdmin ? adminMenu : menu);
    }

    public boolean sendContact(Long chatId, String textToSend) {
        return sendMessage(chatId, textToSend, TelegramMenu.contact());
    }

    private boolean sendMessage(Long chatId, String textToSend, ReplyKeyboard markup) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(textToSend)
                .build();
        if (Objects.nonNull(markup)) {
            message.setReplyMarkup(markup);
        }
        try {
            execute(message);
            return true;
        } catch (TelegramApiException e) {
            log.error("Can't send telegram message", e);
            throw new MessageNotSendException(e);
        }
    }


    @Override
    public String getServiceName() {
        return "telegram";
    }

    @Override
    public boolean sendNotification(TextMessage message) {
        return sendNotification(message.getSetting().getTelegramDialogId(), message.getText(), message.getNotificationId());
    }

    @Override
    public boolean canSendNotification(NotificationSetting setting) {
        return setting.getTelegramEnabled() && Objects.nonNull(setting.getTelegramDialogId());
    }

    public boolean sendFeedback(String from, String text) {
        if (Objects.isNull(properties.getFeedbackChannelId())) {
            return false;
        }
        text = String.format("%s:\n%s", from, text);
        return sendMessage(properties.getFeedbackChannelId(), text, null);
    }
}
