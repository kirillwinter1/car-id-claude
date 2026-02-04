package ru.car.service.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.car.model.FirebaseToken;
import ru.car.model.NotificationSetting;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextMessage {
    private NotificationSetting setting;
    private String phoneNumber;
    private String mail;
    private String text;
    private UUID notificationId;
    private List<FirebaseToken> firebaseTokens;
}
