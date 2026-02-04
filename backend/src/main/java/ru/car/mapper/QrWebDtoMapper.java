package ru.car.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.car.dto.QrDto;
import ru.car.model.Qr;

@Mapper(componentModel = "spring")
public interface QrWebDtoMapper  {

    @Mapping(target = "qrId", source = "id")
    @Mapping(target = "batchNumber", source = "batchId")
    @Mapping(target = "qrName", source = "name")
    @Mapping(target = "userId", ignore = true)
    QrDto toWebDto(Qr qr);
}
