package ru.car.service.message.firebase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushMessage {
    private String token;
    private PushNotification notification;
    private PushApns apns;
}
