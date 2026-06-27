package ru.car.service.message.telegram.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.car.service.message.telegram.TelegramProperties;
import ru.car.service.message.telegram.gateway.dto.GatewayResponse;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;

@Slf4j
@Component
@EnableConfigurationProperties(TelegramGatewayProperties.class)
public class TelegramGatewayService {

    private final TelegramGatewayProperties properties;
    private final RestTemplate restTemplate;

    @Autowired
    public TelegramGatewayService(TelegramGatewayProperties properties, TelegramProperties telegramProperties) {
        this(properties, buildRestTemplate(telegramProperties.getProxy()));
    }

    // package-private — для тестов с MockRestServiceServer
    TelegramGatewayService(TelegramGatewayProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(properties.getEnabled());
    }

    /** @return request_id, если код можно доставить в Telegram; иначе null. */
    public String checkSendAbility(String phoneNumber) {
        try {
            GatewayResponse resp = post("checkSendAbility", Map.of("phone_number", "+" + phoneNumber));
            if (resp != null && resp.isOk() && resp.getResult() != null) {
                return resp.getResult().getRequestId();
            }
            log.warn("Telegram Gateway checkSendAbility не ok для {}: {}",
                    phoneNumber, resp == null ? "null" : resp.getError());
        } catch (Exception e) {
            log.warn("Telegram Gateway checkSendAbility ошибка для {}: {}", phoneNumber, e.getMessage());
        }
        return null;
    }

    /** Доставляет наш код через Telegram. @return true, если Gateway принял (ok=true). */
    public boolean sendCode(String phoneNumber, String code, String requestId) {
        try {
            java.util.Map<String, String> params = new java.util.HashMap<>();
            params.put("phone_number", "+" + phoneNumber);
            params.put("code", code);
            params.put("ttl", String.valueOf(properties.getTtl()));
            if (requestId != null) {
                params.put("request_id", requestId);
            }
            GatewayResponse resp = post("sendVerificationMessage", params);
            boolean ok = resp != null && resp.isOk();
            if (!ok) {
                log.warn("Telegram Gateway sendVerificationMessage не ok для {}: {}",
                        phoneNumber, resp == null ? "null" : resp.getError());
            }
            return ok;
        } catch (Exception e) {
            log.warn("Telegram Gateway sendVerificationMessage ошибка для {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }

    private GatewayResponse post(String method, Map<String, String> params) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(properties.getAccessToken());
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        params.forEach(body::add);
        ResponseEntity<GatewayResponse> resp = restTemplate.exchange(
                properties.getBaseUrl() + "/" + method, HttpMethod.POST,
                new HttpEntity<>(body, headers), GatewayResponse.class);
        return resp.getBody();
    }

    private static RestTemplate buildRestTemplate(TelegramProperties.Proxy proxy) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(20000);
        if (proxy != null && proxy.isEnabled()) {
            factory.setProxy(new Proxy(Proxy.Type.SOCKS,
                    new InetSocketAddress(proxy.getHost(), proxy.getPort())));
        }
        return new RestTemplate(factory);
    }
}
