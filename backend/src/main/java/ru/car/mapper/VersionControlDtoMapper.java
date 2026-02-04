package ru.car.mapper;

import org.mapstruct.Mapper;
import ru.car.dto.VersionControlDto;
import ru.car.model.VersionControl;

@Mapper(componentModel = "spring")
public interface VersionControlDtoMapper extends DtoMapper<VersionControl, VersionControlDto>  {
    VersionControlDto toDto(VersionControl versionControl);
    VersionControl toEntity(VersionControlDto versionControlDto);
}
