package ru.car.mapper;

import org.mapstruct.Mapper;
import ru.car.dto.FeedbackDto;
import ru.car.model.Feedback;

@Mapper(componentModel = "spring")
public interface FeedbackDtoMapper extends DtoMapper<Feedback, FeedbackDto>  {
    FeedbackDto toDto(Feedback feedback);
    Feedback toEntity(FeedbackDto feedbackDto);
}
