package ru.car.service.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.car.constants.ApplicationConstants;
import ru.car.dto.NotificationDto;
import ru.car.model.NotificationSetting;
import ru.car.model.User;
import ru.car.repository.NotificationSettingRepository;
import ru.car.repository.UserRepository;
import ru.car.service.AuthenticationCodeService;
import ru.car.service.MetricService;
import ru.car.service.NotificationService;
import ru.car.service.message.firebase.FirebaseService;
import ru.car.service.message.mail.MailSender;
import ru.car.service.message.sms.SmsService;
import ru.car.service.message.telegram.TelegramBotService;
import ru.car.service.message.whatsapp.WhatsappBotService;
import ru.car.service.message.zvonok.ZvonokService;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageService {
    private final TelegramBotService telegramBotService;
    private final SmsService smsService;
    private final ZvonokService zvonokService;
    private final NotificationSettingRepository notificationSettingRepository;
    private final FirebaseService firebaseService;
    private final WhatsappBotService whatsappBotService;
    private final UserRepository userRepository;
    private final MailSender mailSender;
    private final ExecutorService pushExecutorService;
    private final NotificationService notificationService;
    private final AuthenticationCodeService authenticationCodeService;
    private final MetricService metricService;

    public String sendSmsCode(String telephone, String code) {
        log.info(String.format("%s %s", telephone, code));
//        smsService.send(telephone, "Код авторизации на сервисе car-id: " + code);
        return code;
    }

    public void sendMail(String from, String text) {
        pushExecutorService.execute(() -> {
            send(telegramBotService.getServiceName(), from, text, telegramBotService::sendFeedback);
        });

//        pushExecutorService.execute(() -> {
//            send(mailSender.getServiceName(), from, text, mailSender::sendMessage);
//        });
    }

    private static final Random RANDOM = new SecureRandom();

    public String sendFlashcallCode(String telephone) {
        String code = zvonokService.sendCode(telephone);
//        zvonokService.sendCodeMessage(telephone, String.format("Код для доступа в %s %s . Еще раз %s .", ApplicationConstants.CAR_ID_TITLE, code, code));

        String whatsappText = String.format("Код для доступа в %s: %s", ApplicationConstants.CAR_ID_TITLE, code);
//        pushExecutorService.execute(() -> {
//            send(whatsappBotService.getServiceName(), telephone, whatsappText, whatsappBotService::send);
//        });

        CompletableFuture.runAsync(() -> {
                if (authenticationCodeService.existsCode(telephone, code)) {
                    send(smsService.getServiceName(), telephone, whatsappText, smsService::send);
                }
            }, CompletableFuture.delayedExecutor(30L, TimeUnit.SECONDS));

        return code;
    }

    public String sendCallCode(String telephone) {
        int num = RANDOM.nextInt(9000) + 1000;
        String code = String.valueOf(num);

        CompletableFuture
                .runAsync(() -> {
                    String codeString = String.format("%d %d %d %d", num / 1000, (num % 1000) / 100, (num % 100) / 10, num % 10);
                    zvonokService.sendCodeMessage(telephone, String.format("Код для доступа в %s %s . Еще раз %s .", ApplicationConstants.CAR_ID_TITLE, codeString, codeString));
                }, pushExecutorService);
        return code;
    }

    public String sendWhatsappCallCode(String telephone, String code) {
        int num = RANDOM.nextInt(9000) + 1000;
        String codeString = String.format("%d %d %d %d", num / 1000, (num % 1000) / 100, (num % 100) / 10, num % 10);


        String finalCode = String.valueOf(num);
        code = finalCode;

        CompletableFuture
                .runAsync(() -> {
                    String whatsappText = String.format("Код для доступа в %s: %d", ApplicationConstants.CAR_ID_TITLE, num);
                    send(whatsappBotService.getServiceName(), telephone, whatsappText, whatsappBotService::send);
                }, pushExecutorService)
                .thenRunAsync(() -> {
                    if (authenticationCodeService.existsCode(telephone, finalCode)) {
                        zvonokService.sendCodeMessage(telephone, String.format("Код для доступа в %s %s . Еще раз %s .", ApplicationConstants.CAR_ID_TITLE, codeString, codeString));
                    }
                }, CompletableFuture.delayedExecutor(30L, TimeUnit.SECONDS));
        return code;
    }

    @Transactional
    public void sendPush(NotificationDto dto) {
        metricService.sendNotification();

        NotificationSetting setting = notificationSettingRepository.findByQrId(dto.getQrId());
        User user = userRepository.findById(setting.getUserId()).get();
        TextMessage message = TextMessage.builder()
            .phoneNumber(user.getPhoneNumber())
            .setting(setting)
            .text(dto.getQrName() + ":\n" + dto.getText())
            .notificationId(dto.getNotificationId())
            .firebaseTokens(firebaseService.getFirebaseTokens(setting.getUserId()))
            .build();

        asyncSend(message);
    }

    @Transactional
    public void sendReadPush(NotificationDto dto) {
        if (Objects.isNull(dto.getSenderId())) {
            return ;
        }
        NotificationSetting setting = notificationSettingRepository.findByUserId(dto.getSenderId());
        User user = userRepository.findById(dto.getSenderId()).get();
        TextMessage message = TextMessage.builder()
                .phoneNumber(user.getPhoneNumber())
                .setting(setting)
                .text(String.format("Ваше уведомление \"%s\" прочитано", dto.getText()))
                .notificationId(dto.getNotificationId())
                .firebaseTokens(firebaseService.getFirebaseTokens(dto.getSenderId()))
                .build();

        asyncSend(message);
    }

    @Transactional
    public void sendHelloNewUser(String phoneNumber) {
        if (Objects.isNull(phoneNumber)) {
            return ;
        }
        User user = userRepository.findByPhoneNumber(phoneNumber).get();
        NotificationSetting setting = notificationSettingRepository.findByUserId(user.getId());
        TextMessage message = TextMessage.builder()
                .phoneNumber(user.getPhoneNumber())
                .setting(setting)
                .text(ApplicationConstants.HELLO_PUSH_MESSAGE)
                .firebaseTokens(firebaseService.getFirebaseTokens(user.getId()))
                .build();

        CompletableFuture
                .runAsync(() -> {
                    trySend(firebaseService, message);
                }, CompletableFuture.delayedExecutor(30L, TimeUnit.SECONDS));
    }

    private void asyncSend(TextMessage message) {
        CompletableFuture<Void> send = CompletableFuture
                .runAsync(() -> {
                    trySend(firebaseService, message);
                    trySend(whatsappBotService, message);
                    trySend(telegramBotService, message);
                }, pushExecutorService);

        //если человек подписан на звонки, то через 30 секунд произойдет дозвон
        if (canSend(zvonokService, message)) {
            send.thenRunAsync(() -> {
                    if (notificationService.isUnread(message.getNotificationId())) {
                        trySend(zvonokService, message);
                    }
                }, CompletableFuture.delayedExecutor(30L, TimeUnit.SECONDS)
            );
        }
    }

    public boolean trySend(Sender sender, TextMessage message) {
        if (!canSend(sender, message)) {
            return false;
        }
        try {
            boolean response = sender.sendNotification(message);
            String start = response ? "Отправлено" : "Не отправлено";
            log.info("{} {} уведомление пользователю {} с текстом {}", start, sender.getServiceName(), message.getSetting().getUserId(), message.getText());
            return response;
        } catch (Throwable e) {
            log.error("Ошибка при отправке " + sender.getServiceName() + " уведомления", e);
        }
        return false;
    }

    public boolean canSend(Sender sender, TextMessage message) {
        return sender.canSendNotification(message.getSetting());
    }

    public boolean send(String serviceName, String telephone, String text, BiFunction<String, String, Boolean> send) {
        try {
            boolean response = send.apply(telephone, text);
            String start = response ? "Отправлено" : "Не отправлено";
            log.info("{} {} сообщения пользователю {} с текстом {}", start, serviceName, telephone, text);
            return response;
        } catch (Throwable e) {
            log.error("Ошибка при отправке " + serviceName + " сообщения", e);
        }
        return false;
    }
}
