package ru.car.service.vk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.car.exception.BadRequestException;
import ru.car.test.base.BaseUnitTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("VkIdService Tests")
class VkIdServiceTest extends BaseUnitTest {

    private static final String URL = "https://id.vk.ru/oauth2/user_info";

    private MockRestServiceServer server;
    private VkIdService service;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        service = new VkIdService(new VkIdProperties(true, "12345", URL), restTemplate);
    }

    @Test
    @DisplayName("возвращает нормализованный телефон при ok")
    void returnsNormalizedPhone() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"user\":{\"user_id\":\"42\",\"phone\":\"+7 (999) 123-45-67\"}}",
                        MediaType.APPLICATION_JSON));

        assertThat(service.fetchVerifiedPhone("tok")).isEqualTo("79991234567");
        server.verify();
    }

    @Test
    @DisplayName("бросает VK_AUTH_FAILED, если телефона нет в ответе")
    void throwsWhenNoPhone() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"user\":{\"user_id\":\"42\"}}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.fetchVerifiedPhone("tok"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("бросает VK_AUTH_FAILED при HTTP-ошибке (невалидный токен)")
    void throwsOnHttpError() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> service.fetchVerifiedPhone("tok"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("нормализация: 8XXXXXXXXXX → 7XXXXXXXXXX")
    void normalizesLeadingEight() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"user\":{\"phone\":\"89991234567\"}}", MediaType.APPLICATION_JSON));

        assertThat(service.fetchVerifiedPhone("tok")).isEqualTo("79991234567");
    }
}
