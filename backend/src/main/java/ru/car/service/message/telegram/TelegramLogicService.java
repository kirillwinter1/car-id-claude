package ru.car.service.message.telegram;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.car.constants.ApplicationConstants;
import ru.car.dto.NotificationDto;
import ru.car.dto.QrDto;
import ru.car.enums.BatchTemplates;
import ru.car.exception.AppException;
import ru.car.model.Qr;
import ru.car.model.User;
import ru.car.monitoring.MonitoringService;
import ru.car.repository.BatchRepository;
import ru.car.repository.NotificationSettingRepository;
import ru.car.repository.QrRepository;
import ru.car.service.NotificationFacade;
import ru.car.service.QrService;
import ru.car.service.UserService;
import ru.car.util.MessageUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Slf4j
@Setter
@Component
@RequiredArgsConstructor
public class TelegramLogicService {
    private final TelegramBotService telegramBotService;
    private final UserService userService;
    private final QrRepository qrRepository;
    private final QrService qrService;
    private final NotificationSettingRepository notificationSettingRepository;
    private final MonitoringService monitoringService;
    private final ExecutorService pushExecutorService;
    private final BatchRepository batchRepository;
    private final NotificationFacade notificationFacade;

    @Value("${swagger.server.url}")
    private String url;

    @Transactional
    public void onUpdateReceived(long chatId, Update update) {
        String text = Optional.ofNullable(update.getMessage()).map(Message::getText).orElse("");

        if (!notificationSettingRepository.existsByTelegramDialogId(chatId)) { //диалог не привязан к пользователю
            if (Objects.nonNull(update.getMessage()) && Objects.nonNull(update.getMessage().getContact())) {
                text = update.getMessage().getContact().getPhoneNumber();
            }
            authorization(chatId, text);
            return ;
        }

        Long userId = notificationSettingRepository.findUserIdByTelegramDialogId(chatId);
        User user = userService.getUserOrThrowNotFound(userId);
//
        if (update.hasCallbackQuery()) {
            //работа с кнопками под сообщениями
            String data = update.getCallbackQuery().getData();

            if (data.equals(TelegramMenu.QRS_CMD)) {
                List<Qr> qrs = qrRepository.findByUserId(userId);
                telegramBotService.editMarkup(update.getCallbackQuery().getMessage(), qrs);
            } else if (data.startsWith(TelegramMenu.QR_CMD) && MessageUtils.isUUID(data.replace(TelegramMenu.QR_CMD, ""))) {
                UUID id = UUID.fromString(data.replace(TelegramMenu.QR_CMD, ""));
                //тут (внутри транзакции) надо получить батч для создавния картики
                BatchTemplates template = batchRepository.findByQrId(id).map(b -> b.getTemplate())
                        .orElse(BatchTemplates.PT_WHITE_1);
                pushExecutorService.execute(() -> {
                    telegramBotService.sendQr(update.getCallbackQuery().getMessage().getChatId(), id, template);
                });
            } else if (data.startsWith(TelegramMenu.NOTIFICATION_CMD) && data.replace(TelegramMenu.NOTIFICATION_CMD, "").matches(MessageUtils.UUID_MASK)) {
                UUID id = UUID.fromString(data.replace(TelegramMenu.NOTIFICATION_CMD, ""));
                try {
                    notificationFacade.readBy(NotificationDto.builder().notificationId(id).build(), userId);
                } catch (Exception ignore) {}
                telegramBotService.editMarkup(update.getCallbackQuery().getMessage(), null);
            }
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            // тут уже можно сделать обработчики для различных команд.
            switch (text) {
                case TelegramMenu.REGISTER_USER_CMD -> telegramBotService.sendMessage(chatId, messageForCommand(text), user.isAdmin());
                case TelegramMenu.ACTIVATE_QR_CMD -> telegramBotService.sendMessage(chatId, messageForCommand(text), user.isAdmin());
                case TelegramMenu.READ_NOTIFICATION_CMD  -> telegramBotService.sendMessage(chatId, messageForCommand(text), user.isAdmin());
                case TelegramMenu.SEND_NOTIFICATION_CMD -> telegramBotService.sendMessage(chatId, messageForCommand(text), user.isAdmin());
                case TelegramMenu.QRS_CMD -> telegramBotService.sendQrs(chatId, qrRepository.findByUserId(userId), user.isAdmin());
                case TelegramMenu.TEMPORARY_QR_CMD -> createTemporaryQr(chatId, userId, user.isAdmin());
                default -> telegramBotService.sendMessage(chatId, "Неизвестная команда", user.isAdmin());
            }
        }
    }

    private void createTemporaryQr(long chatId, Long userId, Boolean isAdmin) {
        try {
            QrDto qr = qrService.createTemporaryQr(userId);
            String message = String.format(ApplicationConstants.CREATE_TEMPORARY_QR, qr.getQrId(), url, qr.getQrId());
            telegramBotService.sendMessage(chatId, message, isAdmin);
        } catch (AppException e) {
            telegramBotService.sendMessage(chatId, e.getMessage(), isAdmin);
        } catch (Exception e) {
            telegramBotService.sendMessage(chatId, "внутренняя ошибка при создании временного кода", isAdmin);
            log.error("внутренняя ошибка при создании временного кода", e);
        }
    }

    private void authorization(long chatId, String text) {
        if (!MessageUtils.isValidPhone(text)) {
            telegramBotService.sendContact(chatId, "Введите телефон Вашей учетной записи для авторизации");
            return;
        }
        if (notificationSettingRepository.existsByTelegramDialogId(chatId)) {
            telegramBotService.sendContact(chatId, ApplicationConstants.TELEGRAM_ALREADY_HAS_MESSAGE);
            return;
        }
        Optional<User> user = userService.findByPhoneNumber(MessageUtils.getValidPhone(text));
        if (user.isPresent()) {
            User user1 = user.get();
            notificationSettingRepository.updateTelegramDialogIdByUserId(user1.getId(), chatId);
            telegramBotService.sendMessage(chatId, ApplicationConstants.HELLO_TELEGRAM_MESSAGE, user1.isAdmin());
        } else {
            telegramBotService.sendContact(chatId, "Скачайте мобильное приложение и зарегистрируйте свой телефон");
        }
    }

    private String messageForCommand(String command) {
        String message = "Неизвестная команда";
        switch (command) {
            case TelegramMenu.REGISTER_USER_CMD -> message = String.format("Сегодня зарегистрированно %s пользователей", monitoringService.getRegisterCountToday());
            case TelegramMenu.ACTIVATE_QR_CMD -> message = String.format("Сегодня активировано %s qr", monitoringService.getActivateQrCountToday());
            case TelegramMenu.READ_NOTIFICATION_CMD  -> message = String.format("Сегодня прочитано %s уведомлений", monitoringService.getReadNotificationCountToday());
            case TelegramMenu.SEND_NOTIFICATION_CMD -> message = String.format("Сегодня отправлено %s уведомлений", monitoringService.getSendNotificationCountToday());
        }
        return message;
    }
}
