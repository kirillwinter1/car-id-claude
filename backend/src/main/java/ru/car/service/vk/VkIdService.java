package ru.car.service.vk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.car.enums.ErrorCode;
import ru.car.exception.BadRequestException;
import ru.car.service.vk.dto.VkUserInfoResponse;

@Slf4j
@Component
@EnableConfigurationProperties(VkIdProperties.class)
public class VkIdService {

    private final VkIdProperties properties;
    private final RestTemplate restTemplate;

    @Autowired
    public VkIdService(VkIdProperties properties) {
        this(properties, buildRestTemplate());
    }

    // package-private — для тестов с MockRestServiceServer
    VkIdService(VkIdProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(properties.getEnabled());
    }

    /** Проверяет VK access_token в user_info и возвращает нормализованный телефон. */
    public String fetchVerifiedPhone(String accessToken) {
        VkUserInfoResponse resp;
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", properties.getClientId());
            body.add("access_token", accessToken);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            resp = restTemplate.exchange(properties.getUserInfoUrl(), HttpMethod.POST,
                    new HttpEntity<>(body, headers), VkUserInfoResponse.class).getBody();
        } catch (Exception e) {
            log.warn("VK ID user_info ошибка: {}", e.getMessage());
            throw new BadRequestException("VK auth failed", ErrorCode.VK_AUTH_FAILED);
        }
        String phone = resp != null && resp.getUser() != null ? resp.getUser().getPhone() : null;
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("VK не вернул телефон (нет scope phone?)", ErrorCode.VK_AUTH_FAILED);
        }
        return normalizePhone(phone);
    }

    /** Приводит к виду 7XXXXXXXXXX (как users.phone_number). */
    static String normalizePhone(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 11 && digits.startsWith("8")) {
            digits = "7" + digits.substring(1);
        }
        if (digits.length() == 10) {
            digits = "7" + digits;
        }
        return digits;
    }

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }
}
