package ru.car.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.car.dto.NotificationSettingDto;
import ru.car.model.NotificationSetting;

@Mapper(componentModel = "spring")
public interface NotificationSettingDtoMapper extends DtoMapper<NotificationSetting, NotificationSettingDto>  {
    NotificationSettingDto toDto(NotificationSetting notificationSetting);
    NotificationSetting toEntity(NotificationSettingDto notificationSettingDto);

    @Mapping(target = "pushEnabled", source = "pushEnabled", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "callEnabled", source = "callEnabled", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "telegramEnabled", source = "telegramEnabled", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "whatsappEnabled", source = "whatsappEnabled", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "active", source = "active", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "telegramDialogId", source = "telegramDialogId", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateIgnoreNull(NotificationSettingDto dto, @MappingTarget NotificationSetting setting);
}
