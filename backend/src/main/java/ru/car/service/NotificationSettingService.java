package ru.car.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.car.dto.NotificationSettingDto;
import ru.car.enums.ErrorCode;
import ru.car.exception.ForbiddenException;
import ru.car.mapper.NotificationSettingDtoMapper;
import ru.car.model.NotificationSetting;
import ru.car.repository.NotificationSettingRepository;
import ru.car.service.message.telegram.TelegramMenu;
import ru.car.service.message.telegram.TelegramProperties;
import ru.car.service.message.whatsapp.WhatsappBotService;
import ru.car.service.security.AuthService;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class NotificationSettingService {
    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationSettingDtoMapper notificationSettingDtoMapper;
    private final WhatsappBotService whatsappBotService;
    private final TelegramProperties telegramProperties;
    private final AuthService authService;

    @Transactional
    public NotificationSettingDto get(Long userId) {
        return notificationSettingDtoMapper.toDto(notificationSettingRepository.findByUserId(userId));
    }

    @Transactional
    public void create(Long userId) {
        notificationSettingRepository.save(NotificationSetting.builder()
                .userId(userId)
                .pushEnabled(true)
                .callEnabled(false)
                .whatsappEnabled(false)
                .telegramEnabled(false)
                .active(true)
                .build());
    }

    @Transactional
    public NotificationSettingDto patch(Long userId, NotificationSettingDto dto) {
        NotificationSetting setting = notificationSettingRepository.findByUserId(userId);
        checkWhatsappEnabledValue(setting.getWhatsappEnabled(), dto.getWhatsappEnabled());
        checkTelegramEnabledValue(setting.getTelegramEnabled(), dto.getTelegramEnabled(), setting.getTelegramDialogId());
        notificationSettingDtoMapper.updateIgnoreNull(dto, setting);
        setting = notificationSettingRepository.update(setting);
        return notificationSettingDtoMapper.toDto(setting);
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        notificationSettingRepository.update(NotificationSetting.builder()
                .userId(userId)
                .pushEnabled(true)
                .callEnabled(false)
                .whatsappEnabled(false)
                .telegramEnabled(false)
                .active(false)
                .telegramDialogId(null)
                .build());
    }

    private void checkWhatsappEnabledValue(Boolean oldVal, Boolean newVal) {
        if (Objects.isNull(newVal) || oldVal.equals(newVal)) {
            return ;
        }
        if (newVal && !whatsappBotService.check(authService.getPhoneNumber())) {
            throw new ForbiddenException(ErrorCode.WHATSAPP_AUTH_ERROR.getDescription(), ErrorCode.WHATSAPP_AUTH_ERROR);
        }
    }

    private void checkTelegramEnabledValue(Boolean oldVal, Boolean newVal, Long telegramDialogId) {
        if (Objects.isNull(newVal) || oldVal.equals(newVal)) {
            return ;
        }
        if (newVal && Objects.isNull(telegramDialogId)) {
            throw new ForbiddenException(ErrorCode.TELEGRAM_AUTH_ERROR.getDescription(), ErrorCode.TELEGRAM_AUTH_ERROR, telegramProperties.getBot(), TelegramMenu.CONTACT_CMD);
        }
    }
}
