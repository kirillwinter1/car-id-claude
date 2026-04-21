package ru.car.service.message.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.car.constants.ApplicationConstants;
import ru.car.exception.MessageNotSendException;
import ru.car.model.FirebaseToken;
import ru.car.model.NotificationSetting;
import ru.car.repository.FirebaseTokenRepository;
import ru.car.service.message.Sender;
import ru.car.service.message.TextMessage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseService implements Sender {

    private final FirebaseProperties properties;
    private final FirebaseTokenRepository firebaseTokenRepository;
    private GoogleCredentials googleCredentials;

    @PostConstruct
    public void init() throws IOException {
        googleCredentials = GoogleCredentials
                .fromStream(FirebaseService.class.getResourceAsStream(properties.getSourceCredentials()))
                .createScoped(properties.getScopeCredentials());
        googleCredentials.refresh();
    }

    @SneakyThrows
    private String getAccessToken() {
        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }

    private boolean send(String userToken, String text) {
        Push push = Push.builder()
                .message(PushMessage.builder()
                        .token(userToken)
                        .notification(PushNotification.builder()
                                .body(text)
                                .title(ApplicationConstants.CAR_ID_TITLE)
                                .build())
                        .apns(PushApns.builder()
                                .payload(PushPayload.builder()
                                        .aps(PushAps.builder()
                                                .sound("default")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();


        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAccessToken());

        HttpEntity<Push> request = new HttpEntity<>(push, headers);
        ResponseEntity<Map> response;

        log.debug("push service request : [{}]", request);
        try {
            response = restTemplate.postForEntity(properties.getUrl(), request, Map.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Не удалось отправить сообщение в firebase: {}", response.getBody());
                throw new MessageNotSendException("Не удалось отправить сообщение в firebase: %s", response.getBody());
            }
            log.debug("push service response : [{}]", response.getBody());
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new MessageNotSendException(e);
        }
    }

    @Override
    public boolean sendNotification(TextMessage message) {
        boolean success = false;
        for (FirebaseToken token : CollectionUtils.emptyIfNull(message.getFirebaseTokens())) {
            try {
                success |= send(token.getToken(), message.getText());
            } catch (Exception e) {
                log.warn("FCM send failed for token id={} userId={}: {}",
                        token.getId(), token.getUserId(), e.getMessage());
            }
        }
        return success;
    }

    public List<FirebaseToken> getFirebaseTokens(Long userId) {
        return firebaseTokenRepository.findAllByUserId(userId);
    }

    @Override
    public boolean canSendNotification(NotificationSetting setting) {
        return setting.getPushEnabled();
    }

    @Override
    public String getServiceName() {
        return "firebase";
    }

}
