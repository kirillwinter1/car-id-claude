package ru.car.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.car.mapper.NotificationSettingDtoMapper;
import ru.car.model.NotificationSetting;
import ru.car.repository.NotificationSettingRepository;
import ru.car.service.message.telegram.TelegramProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSettingService Tests")
class NotificationSettingServiceTest {

    @Mock
    NotificationSettingRepository notificationSettingRepository;

    @Mock
    NotificationSettingDtoMapper notificationSettingDtoMapper;

    @Mock
    TelegramProperties telegramProperties;

    @InjectMocks
    NotificationSettingService service;

    private NotificationSetting setting;

    @BeforeEach
    void setUp() {
        setting = NotificationSetting.builder()
                .userId(42L)
                .pushEnabled(true)
                .callEnabled(false)
                .telegramEnabled(true)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("toggleChannel")
    class ToggleChannel {

        @Test
        @DisplayName("push: flips pushEnabled and keeps others unchanged")
        void toggleChannel_push_flipsPushAndKeepsOthers() {
            when(notificationSettingRepository.findByUserId(42L)).thenReturn(setting);
            when(notificationSettingRepository.update(any(NotificationSetting.class)))
                    .thenAnswer(i -> i.getArgument(0));

            NotificationSetting result = service.toggleChannel(42L, "push");

            assertThat(result.getPushEnabled()).isFalse();
            assertThat(result.getCallEnabled()).isFalse();
            assertThat(result.getTelegramEnabled()).isTrue();
        }

        @Test
        @DisplayName("call: flips callEnabled")
        void toggleChannel_call_flipsCall() {
            when(notificationSettingRepository.findByUserId(42L)).thenReturn(setting);
            when(notificationSettingRepository.update(any(NotificationSetting.class)))
                    .thenAnswer(i -> i.getArgument(0));

            assertThat(service.toggleChannel(42L, "call").getCallEnabled()).isTrue();
        }

        @Test
        @DisplayName("telegram: flips telegramEnabled")
        void toggleChannel_telegram_flipsTelegram() {
            when(notificationSettingRepository.findByUserId(42L)).thenReturn(setting);
            when(notificationSettingRepository.update(any(NotificationSetting.class)))
                    .thenAnswer(i -> i.getArgument(0));

            assertThat(service.toggleChannel(42L, "telegram").getTelegramEnabled()).isFalse();
        }

        @Test
        @DisplayName("unknown channel: throws IllegalArgumentException")
        void toggleChannel_unknown_throws() {
            assertThatThrownBy(() -> service.toggleChannel(42L, "xxx"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
