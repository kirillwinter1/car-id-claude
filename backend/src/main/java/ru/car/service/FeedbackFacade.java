package ru.car.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.car.dto.FeedbackDto;
import ru.car.enums.FeedbackChannels;
import ru.car.service.message.MessageService;
import ru.car.service.security.AuthService;

@Component
@RequiredArgsConstructor
public class FeedbackFacade {
    private final FeedbackService feedbackService;
    private final MessageService messageService;
    private final AuthService authService;

    public FeedbackDto send(FeedbackDto dto) {
        dto.setChannel(FeedbackChannels.APP);
        return send(authService.getUserId(), dto);
    }

    public FeedbackDto sendAnonymous(FeedbackDto dto) {
        dto.setChannel(FeedbackChannels.WEB);
        return send(null, dto);
    }

    private FeedbackDto send(Long userId, FeedbackDto dto) {
        FeedbackDto response = feedbackService.create(userId, dto);
        messageService.sendMail(response.getEmail(), response.getText());
        return response;
    }
}
