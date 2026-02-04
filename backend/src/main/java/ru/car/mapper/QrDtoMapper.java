package ru.car.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.car.dto.QrDto;
import ru.car.model.Qr;

@Mapper(componentModel = "spring")
public interface QrDtoMapper extends DtoMapper<Qr, QrDto>  {

    @Mapping(target = "qrId", source = "id")
    @Mapping(target = "batchNumber", source = "batchId")
    @Mapping(target = "qrName", source = "name")
    QrDto toDto(Qr qr);

    @Mapping(target = "id", source = "qrId")
    @Mapping(target = "batchId", source = "batchNumber")
    @Mapping(target = "name", source = "qrName")
    Qr toEntity(QrDto qrDto);
}
