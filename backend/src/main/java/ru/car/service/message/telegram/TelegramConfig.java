package ru.car.service.message.telegram;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.car.service.NotificationFacade;

@Configuration
@RequiredArgsConstructor
public class TelegramConfig {

    private final TelegramBotService telegramBotService;
    private final TelegramLogicService telegramLogicService;


    @PostConstruct
    void initMessageService() throws TelegramApiException {
        telegramBotService.setTelegramLogicService(telegramLogicService);
        telegramBotService.init();
    }
}
