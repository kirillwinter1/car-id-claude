package ru.car.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.car.enums.FeedbackChannels;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {
    private Long id;
    private Long userId;
    private FeedbackChannels channel;
    private String email;
    private String text;
    private LocalDateTime createdDate;
}
