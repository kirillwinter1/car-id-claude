package ru.car.service.message.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;
import ru.car.exception.MessageNotSendException;
import ru.car.model.NotificationSetting;
import ru.car.service.message.Sender;
import ru.car.service.message.TextMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsService implements Sender {

    // Get credentials from account settings page: https://smsaero.ru/cabinet/settings/apikey/
    private static final String email = "t89043139690@mail.ru";
    private static final String apiKey = "-6nLeBPieJi8RsI_lv5dY2pZrz1Esm1T";
    private static final String sign = "SmsAero";

    public boolean send(String telephone, String text) {
        SmsAeroClient client = new SmsAeroClient(email, apiKey);

        try {
            JSONObject balanceResult = client.Balance();
            JSONObject data = (JSONObject) balanceResult.get("data");
            if (data.get("balance").equals(0.00)) {
                System.out.println("Insufficient balance.");
                return false;
            }

            JSONObject sendResult = client.Send(telephone, text, sign);
            if (sendResult == null) {
                log.info("Can not send sms.");
                return false;
            } else if (sendResult.get("success").equals(false)) {
                log.info("Don't sent. {}", sendResult.get("reason"));
                return false;
            }

            data = (JSONObject) sendResult.get("data");
            log.info("Successfully sent. {}", data.toString());
            return true;
        } catch (Exception e) {
            throw new MessageNotSendException(e);
        }
    }


    @Override
    public String getServiceName() {
        return "smsaero";
    }

    @Override
    public boolean sendNotification(TextMessage message) {
        return send(message.getPhoneNumber(), message.getText());
    }

    @Override
    public boolean canSendNotification(NotificationSetting setting) {
        return true;
    }
}