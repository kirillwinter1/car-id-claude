package ru.car.service.message;

import ru.car.model.NotificationSetting;

public interface Sender {

    String getServiceName();
    boolean sendNotification(TextMessage message);
    boolean canSendNotification(NotificationSetting setting);
}
