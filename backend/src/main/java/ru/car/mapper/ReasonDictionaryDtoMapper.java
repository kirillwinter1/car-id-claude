package ru.car.mapper;

import org.mapstruct.Mapper;
import ru.car.dto.ReasonDictionaryDto;
import ru.car.model.ReasonDictionary;

@Mapper(componentModel = "spring")
public interface ReasonDictionaryDtoMapper extends DtoMapper<ReasonDictionary, ReasonDictionaryDto>  {
}
