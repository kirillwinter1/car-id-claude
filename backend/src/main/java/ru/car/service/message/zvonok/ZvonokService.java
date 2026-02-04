package ru.car.service.message.zvonok;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.car.exception.MessageNotSendException;
import ru.car.model.NotificationSetting;
import ru.car.service.NotificationService;
import ru.car.service.message.Sender;
import ru.car.service.message.TextMessage;

import java.util.Objects;


@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(ZvonokProperties.class)
public class ZvonokService implements Sender {
    private final ZvonokProperties zvonokProperties;
    private final NotificationService notificationService;
    private RestTemplate restTemplate = new RestTemplate();

    public String sendCode(String phoneNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("campaign_id", zvonokProperties.getFlashcallCampaignId());
        map.add("phone", "+" + phoneNumber);
        map.add("public_key", zvonokProperties.getPublicKeyApi());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<FlashcallDto> exchange = restTemplate.exchange(zvonokProperties.getUrlFlashcall(), HttpMethod.POST, entity, FlashcallDto.class);
            if (exchange.getStatusCode().is2xxSuccessful() && exchange.getBody().getStatus().equalsIgnoreCase("ok")) {
                String code = exchange.getBody().getData().getPincode();
                log.info("Успешно отправлен код {} через {} на телефон {}", code, getServiceName(), phoneNumber);
                return code;
            } else {
                log.error("Что-то пошло не так при вызове сервиса обзвонов code {} body {}", exchange.getStatusCode(), exchange.getBody());
                throw new MessageNotSendException("Что-то пошло не так при вызове сервиса обзвонов code %s: [%s]", exchange.getStatusCode(), exchange.getBody());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new MessageNotSendException("Что-то пошло не так при вызове сервиса обзвонов %s: [%s]", e.getMessage(), e);
        }
    }

    public boolean sendCodeMessage(String phoneNumber, String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("campaign_id", zvonokProperties.getCodeCallCampaignId());
        map.add("phone", "+" + phoneNumber);
        map.add("public_key", zvonokProperties.getPublicKeyApi());
        map.add("text", text);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<CallData> exchange = restTemplate.exchange(zvonokProperties.getUrlCall(), HttpMethod.POST, entity, CallData.class);
            log.info(exchange.getBody().toString());
            if (exchange.getStatusCode().is2xxSuccessful()) {
                return true;
            } else {
                log.error("Что-то пошло не так при вызове сервиса обзвонов code {} body {}", exchange.getStatusCode(), exchange.getBody());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    public CallData sendMessage(String phoneNumber, String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("campaign_id", zvonokProperties.getCallCampaignId());
        map.add("phone", "+" + phoneNumber);
        map.add("public_key", zvonokProperties.getPublicKeyApi());
        map.add("text", text);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<CallData> exchange = restTemplate.exchange(zvonokProperties.getUrlCall(), HttpMethod.POST, entity, CallData.class);
            log.info(exchange.getBody().toString());
            if (exchange.getStatusCode().is2xxSuccessful()) {
                return exchange.getBody();
            } else {
                log.error("Что-то пошло не так при вызове сервиса обзвонов code {} body {}", exchange.getStatusCode(), exchange.getBody());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String getServiceName() {
        return "zvonok";
    }

    @Override
    public boolean sendNotification(TextMessage message) {
        String text = "Вам звонок из car id . " + message.getText();
        CallData callData = sendMessage(message.getPhoneNumber(), text);
        if (Objects.isNull(callData)) {
            return false;
        }
        if (Objects.nonNull(callData.getCall_id())) {
            notificationService.updateCallId(message.getNotificationId(), callData.getCall_id());
        }
        return true;
    }

    @Override
    public boolean canSendNotification(NotificationSetting setting) {
        return setting.getCallEnabled();
    }
}
