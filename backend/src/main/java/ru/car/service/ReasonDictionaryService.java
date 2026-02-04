package ru.car.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.car.dto.ReasonDictionaryDto;
import ru.car.exception.NotFoundException;
import ru.car.mapper.ReasonDictionaryDtoMapper;
import ru.car.model.ReasonDictionary;
import ru.car.repository.ReasonDictionaryRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReasonDictionaryService {
    private final ReasonDictionaryRepository reasonDictionaryRepository;
    private final ReasonDictionaryDtoMapper reasonDictionaryDtoMapper;

    public ReasonDictionary findByIdOrThrowNotFound(Long id) {
        return reasonDictionaryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Не найдена причина с id = %s", id));
    }

    @Transactional
    public List<ReasonDictionaryDto> findAll() {
        return reasonDictionaryDtoMapper.toDto(reasonDictionaryRepository.findAll());
    }
}
