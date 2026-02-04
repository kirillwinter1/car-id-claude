package ru.car.mapper;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.car.dto.NotificationDto;
import ru.car.model.Notification;
import ru.car.model.Qr;
import ru.car.model.ReasonDictionary;

import java.util.Optional;

@Mapper(componentModel = "spring")
public interface NotificationDtoMapper extends DtoMapper<Notification, NotificationDto>  {

    @Mapping(target = "notificationId", source = "id")
    @Mapping(target = "time", source = "createdDate")
    @Mapping(target = "qrName", expression = "java(fillQrName(notification))")
    @Mapping(target = "text", expression = "java(fillText(notification))")
    NotificationDto toDto(Notification notification);

    @Mapping(target = "id", source = "notificationId")
    @Mapping(target = "createdDate", source = "time")
    Notification toEntity(NotificationDto notificationDto);

    default String fillText(Notification notification) {
        return StringUtils.isNoneEmpty(notification.getText())
                ? notification.getText()
                : Optional.ofNullable(notification.getReason())
                    .map(ReasonDictionary::getDescription)
                    .orElse(null);
    }

    default String fillQrName(Notification notification) {
        return Optional.ofNullable(notification.getQr())
                .map(Qr::getName)
                .orElse(null);
    }
}
