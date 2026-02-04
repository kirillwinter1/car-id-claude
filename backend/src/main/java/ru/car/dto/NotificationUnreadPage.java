package ru.car.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationUnreadPage {
    private Integer page;
    private Integer size;
    private Integer count;
    private List<NotificationDto> notifications;
}
