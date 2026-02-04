package ru.car.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.car.dto.FeedbackDto;
import ru.car.enums.FeedbackChannels;
import ru.car.mapper.FeedbackDtoMapper;
import ru.car.model.Feedback;
import ru.car.repository.FeedbackRepository;

@Component
@RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final FeedbackDtoMapper feedbackDtoMapper;

    @Transactional
    public FeedbackDto create(Long userId, FeedbackDto dto) {
        return feedbackDtoMapper.toDto(feedbackRepository.save(Feedback.builder()
                .email(dto.getEmail())
                .text(dto.getText())
                .channel(dto.getChannel())
                .userId(userId)
                .build()));
    }
}
