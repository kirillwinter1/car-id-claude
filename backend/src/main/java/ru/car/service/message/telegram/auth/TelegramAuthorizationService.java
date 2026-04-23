package ru.car.service.message.telegram.auth;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.car.model.User;
import ru.car.repository.NotificationSettingRepository;
import ru.car.service.UserService;
import ru.car.service.message.telegram.render.TelegramMessages;
import ru.car.service.message.telegram.scene.SceneOutput;
import ru.car.util.MessageUtils;

import java.util.List;
import java.util.Optional;

@Service
public class TelegramAuthorizationService {

    private final UserService userService;
    private final NotificationSettingRepository settingRepository;
    private final TelegramMessages messages;
    private final TelegramStartTokenService startTokenService;

    public TelegramAuthorizationService(UserService userService,
                                         NotificationSettingRepository settingRepository,
                                         TelegramMessages messages,
                                         TelegramStartTokenService startTokenService) {
        this.userService = userService;
        this.settingRepository = settingRepository;
        this.messages = messages;
        this.startTokenService = startTokenService;
    }

    public SceneOutput handle(long chatId, String text) {
        if (settingRepository.existsByTelegramDialogId(chatId)) {
            return SceneOutput.send(messages.get("tg.auth.already_linked"), contactKeyboard());
        }
        if (text != null && !text.isBlank() && !MessageUtils.isValidPhone(text)) {
            Optional<Long> userIdOpt = startTokenService.verify(text);
            if (userIdOpt.isPresent()) {
                Optional<User> user = userService.findById(userIdOpt.get());
                if (user.isPresent()) {
                    settingRepository.updateTelegramDialogIdByUserId(user.get().getId(), chatId);
                    ReplyKeyboardRemove remove = ReplyKeyboardRemove.builder()
                        .removeKeyboard(true).build();
                    return SceneOutput.send(messages.get("tg.auth.welcome_deep_link"), remove);
                }
                return SceneOutput.send(messages.get("tg.auth.token_user_not_found"), contactKeyboard());
            }
        }
        if (!MessageUtils.isValidPhone(text)) {
            return SceneOutput.send(messages.get("tg.auth.request_contact"), contactKeyboard());
        }
        Optional<User> user = userService.findByPhoneNumber(MessageUtils.getValidPhone(text));
        if (user.isEmpty()) {
            return SceneOutput.send(messages.get("tg.auth.not_registered"), contactKeyboard());
        }
        settingRepository.updateTelegramDialogIdByUserId(user.get().getId(), chatId);
        ReplyKeyboardRemove remove = ReplyKeyboardRemove.builder()
            .removeKeyboard(true).build();
        return SceneOutput.send(messages.get("tg.auth.welcome"), remove);
    }

    private ReplyKeyboardMarkup contactKeyboard() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false);
        KeyboardButton button = new KeyboardButton(messages.get("tg.auth.btn.share_contact"));
        button.setRequestContact(true);
        markup.setKeyboard(List.of(new KeyboardRow(List.of(button))));
        return markup;
    }
}
