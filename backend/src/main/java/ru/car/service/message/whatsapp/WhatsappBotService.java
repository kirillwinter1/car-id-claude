package ru.car.service.message.whatsapp;

import com.greenapi.pkg.api.GreenApi;
import com.greenapi.pkg.models.request.OutgoingMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.car.model.NotificationSetting;
import ru.car.service.message.Sender;
import ru.car.service.message.TextMessage;

@Component
@RequiredArgsConstructor
public class WhatsappBotService implements Sender {
    private final GreenApi greenApi;

    public boolean send(String telephone, String text) {
        return greenApi.sending.sendMessage(OutgoingMessage.builder()
                    .chatId(telephone + "@c.us")
                    .message(text)
                    .build())
                .getStatusCode().is2xxSuccessful();
    }

    public boolean check(String telephone) {
        try {
            return greenApi.service.checkWhatsapp(Long.parseLong(telephone)).getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean sendNotification(TextMessage message) {
        return send(message.getPhoneNumber(), message.getText());
    }

    @Override
    public boolean canSendNotification(NotificationSetting setting) {
        return setting.getWhatsappEnabled();
    }

    @Override
    public String getServiceName() {
        return "whatsapp";
    }
}
