package ru.car.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.car.dto.VersionControlDto;
import ru.car.mapper.VersionControlDtoMapper;
import ru.car.repository.VersionControlRepository;

@Component
@RequiredArgsConstructor
public class VersionControlService {

    private final VersionControlRepository versionControlRepository;
    private final VersionControlDtoMapper versionControlDtoMapper;

    public VersionControlDto get() {
        return versionControlDtoMapper.toDto(versionControlRepository.findFirst());
    }
}
