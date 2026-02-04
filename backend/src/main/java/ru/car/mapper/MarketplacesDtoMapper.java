package ru.car.mapper;

import org.mapstruct.Mapper;
import ru.car.dto.MarketplacesDto;
import ru.car.model.Marketplaces;

@Mapper(componentModel = "spring")
public interface MarketplacesDtoMapper extends DtoMapper<Marketplaces, MarketplacesDto>  {
    MarketplacesDto toDto(Marketplaces Marketplaces);
    Marketplaces toEntity(MarketplacesDto MarketplacesDto);
}
