package ru.car.service.message.telegram.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.car.test.base.BaseUnitTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("TelegramGatewayService Tests")
class TelegramGatewayServiceTest extends BaseUnitTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private TelegramGatewayService service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        TelegramGatewayProperties props = new TelegramGatewayProperties(
                true, "https://gatewayapi.telegram.org", "TOKEN", 300);
        service = new TelegramGatewayService(props, restTemplate);
    }

    @Test
    @DisplayName("checkSendAbility возвращает request_id при ok=true")
    void checkSendAbilityReturnsRequestId() {
        server.expect(requestTo("https://gatewayapi.telegram.org/checkSendAbility"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer TOKEN"))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"request_id\":\"req-123\"}}",
                        MediaType.APPLICATION_JSON));

        assertThat(service.checkSendAbility("79991234567")).isEqualTo("req-123");
        server.verify();
    }

    @Test
    @DisplayName("checkSendAbility возвращает null при ok=false (нет Telegram)")
    void checkSendAbilityReturnsNullWhenNotOk() {
        server.expect(requestTo("https://gatewayapi.telegram.org/checkSendAbility"))
                .andRespond(withSuccess("{\"ok\":false,\"error\":\"PHONE_NOT_TELEGRAM\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(service.checkSendAbility("79991234567")).isNull();
    }

    @Test
    @DisplayName("checkSendAbility возвращает null при HTTP-ошибке")
    void checkSendAbilityReturnsNullOnHttpError() {
        server.expect(requestTo("https://gatewayapi.telegram.org/checkSendAbility"))
                .andRespond(withServerError());

        assertThat(service.checkSendAbility("79991234567")).isNull();
    }

    @Test
    @DisplayName("sendCode возвращает true при ok=true и шлёт code/ttl/request_id")
    void sendCodeReturnsTrue() {
        server.expect(requestTo("https://gatewayapi.telegram.org/sendVerificationMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer TOKEN"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .content().formData(formData()))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"request_id\":\"req-123\"}}",
                        MediaType.APPLICATION_JSON));

        assertThat(service.sendCode("79991234567", "1234", "req-123")).isTrue();
        server.verify();
    }

    private static org.springframework.util.MultiValueMap<String, String> formData() {
        org.springframework.util.MultiValueMap<String, String> m =
                new org.springframework.util.LinkedMultiValueMap<>();
        m.add("phone_number", "+79991234567");
        m.add("code", "1234");
        m.add("ttl", "300");
        m.add("request_id", "req-123");
        return m;
    }

    @Test
    @DisplayName("sendCode возвращает false при ok=false")
    void sendCodeReturnsFalseWhenNotOk() {
        server.expect(requestTo("https://gatewayapi.telegram.org/sendVerificationMessage"))
                .andRespond(withSuccess("{\"ok\":false,\"error\":\"BALANCE_NOT_ENOUGH\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(service.sendCode("79991234567", "1234", "req-123")).isFalse();
    }

    @Test
    @DisplayName("sendCode возвращает false при HTTP-ошибке")
    void sendCodeReturnsFalseOnHttpError() {
        server.expect(requestTo("https://gatewayapi.telegram.org/sendVerificationMessage"))
                .andRespond(withServerError());

        assertThat(service.sendCode("79991234567", "1234", "req-123")).isFalse();
    }
}
