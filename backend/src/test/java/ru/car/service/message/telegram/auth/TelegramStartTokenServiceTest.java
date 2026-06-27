package ru.car.service.message.telegram.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramStartTokenServiceTest {

    // 32 raw bytes base64 = "c2VjcmV0LWJhc2U2NC1rZXktMzJieXRlcy1sb25nLXRyaWFsISE=" (>= 32 bytes)
    private final TelegramStartTokenService svc = new TelegramStartTokenService(
        "c2VjcmV0LWJhc2U2NC1rZXktMzJieXRlcy1sb25nLXRyaWFsISE=");

    @Test
    void signAndVerify_roundTrip() {
        String token = svc.sign(42L, Duration.ofMinutes(5));
        assertThat(svc.verify(token)).contains(42L);
    }

    @Test
    void verify_tamperedPayload_returnsEmpty() {
        String token = svc.sign(42L, Duration.ofMinutes(5));
        // подменить первый символ в payload (до '_')
        String tampered = (token.charAt(0) == 'X' ? 'Y' : 'X') + token.substring(1);
        assertThat(svc.verify(tampered)).isEmpty();
    }

    @Test
    void verify_tamperedSignature_returnsEmpty() {
        String token = svc.sign(42L, Duration.ofMinutes(5));
        // Меняем ПЕРВЫЙ символ base64-подписи (после '_'): в нём биты всегда значимы.
        // Флип последнего символа был флейки — в нём «незначащие» биты неполной base64-группы
        // могли декодироваться в те же байты HMAC, и подпись оставалась валидной.
        int sep = token.indexOf('_');
        char sigFirst = token.charAt(sep + 1);
        String tampered = token.substring(0, sep + 1) + (sigFirst == 'A' ? 'B' : 'A') + token.substring(sep + 2);
        assertThat(svc.verify(tampered)).isEmpty();
    }

    @Test
    void verify_expired_returnsEmpty() throws InterruptedException {
        String token = svc.sign(42L, Duration.ofMillis(1));
        Thread.sleep(1100); // wait past TTL
        assertThat(svc.verify(token)).isEmpty();
    }

    @Test
    void verify_blank_returnsEmpty() {
        assertThat(svc.verify("")).isEmpty();
        assertThat(svc.verify(null)).isEmpty();
        assertThat(svc.verify("no_underscore")).isEmpty();
    }

    @Test
    void verify_differentSecret_returnsEmpty() {
        TelegramStartTokenService other = new TelegramStartTokenService(
            "b3RoZXItc2VjcmV0LWtleS0zMmJ5dGVzLWxvbmctdHJpYWwhIS4=");
        String token = svc.sign(42L, Duration.ofMinutes(5));
        assertThat(other.verify(token)).isEmpty();
    }

    @Test
    void tokenFitsTelegramBudget() {
        String token = svc.sign(Long.MAX_VALUE, Duration.ofDays(365));
        assertThat(token.length()).isLessThanOrEqualTo(64);
        assertThat(token).matches("[A-Za-z0-9_-]+");
    }
}
