package ru.car.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.car.dto.MarketplacesDto;
import ru.car.mapper.MarketplacesDtoMapper;
import ru.car.repository.MarketplacesRepository;

@Component
@RequiredArgsConstructor
public class MarketplaceService {

    private final MarketplacesRepository marketplacesRepository;
    private final MarketplacesDtoMapper marketplacesDtoMapper;

    public MarketplacesDto get() {
        return marketplacesDtoMapper.toDto(marketplacesRepository.findFirst());
    }
}
