package ru.car.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import ru.car.dto.TelegramStartUrlDto;
import ru.car.dto.mobile.MobileRs;
import ru.car.service.message.telegram.TelegramProperties;
import ru.car.service.message.telegram.auth.TelegramStartTokenService;
import ru.car.service.security.AuthService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramAuthControllerTest {

    @Mock TelegramStartTokenService tokenService;
    @Mock AuthService authService;

    TelegramAuthController controller;

    @BeforeEach
    void setUp() {
        TelegramProperties props = new TelegramProperties("car_id_test_bot", "tok", 0L, true);
        controller = new TelegramAuthController(tokenService, props, authService);
    }

    @Test
    void getStartUrl_returnsSignedDeepLink() {
        when(authService.getUserId()).thenReturn(42L);
        when(tokenService.sign(eq(42L), any(Duration.class))).thenReturn("TOKEN_XYZ");

        ResponseEntity<MobileRs<TelegramStartUrlDto>> response = controller.getStartUrl();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResult()).isEqualTo("true");
        assertThat(response.getBody().getParams().getUrl())
            .isEqualTo("https://t.me/car_id_test_bot?start=TOKEN_XYZ");
    }
}
