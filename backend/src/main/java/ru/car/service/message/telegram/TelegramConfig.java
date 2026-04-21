package ru.car.service.message.telegram;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Configuration
@RequiredArgsConstructor
public class TelegramConfig {

    private final TelegramBotService telegramBotService;

    @PostConstruct
    void initTelegramBot() throws TelegramApiException {
        telegramBotService.init();
    }
}
