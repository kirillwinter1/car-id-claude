package ru.car.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.car.dto.NotificationSettingDto;
import ru.car.model.NotificationSetting;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationSettingDtoMapper Tests")
class NotificationSettingDtoMapperTest {

    private final NotificationSettingDtoMapper mapper = new NotificationSettingDtoMapperImpl();

    @Test
    @DisplayName("updateIgnoreNull копирует showPhoneOnUnreachable")
    void updateIgnoreNullCopiesShowPhone() {
        NotificationSetting setting = NotificationSetting.builder().showPhoneOnUnreachable(false).build();
        NotificationSettingDto dto = NotificationSettingDto.builder().showPhoneOnUnreachable(true).build();

        mapper.updateIgnoreNull(dto, setting);

        assertThat(setting.getShowPhoneOnUnreachable()).isTrue();
    }

    @Test
    @DisplayName("updateIgnoreNull не перезатирает showPhoneOnUnreachable значением null")
    void updateIgnoreNullKeepsExistingWhenNull() {
        NotificationSetting setting = NotificationSetting.builder().showPhoneOnUnreachable(true).build();
        NotificationSettingDto dto = NotificationSettingDto.builder().showPhoneOnUnreachable(null).build();

        mapper.updateIgnoreNull(dto, setting);

        assertThat(setting.getShowPhoneOnUnreachable()).isTrue();
    }

    @Test
    @DisplayName("toDto переносит showPhoneOnUnreachable")
    void toDtoMapsShowPhone() {
        NotificationSetting setting = NotificationSetting.builder().showPhoneOnUnreachable(true).build();

        assertThat(mapper.toDto(setting).getShowPhoneOnUnreachable()).isTrue();
    }
}
