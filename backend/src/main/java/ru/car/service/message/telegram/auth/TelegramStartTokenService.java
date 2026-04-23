package ru.car.service.message.telegram.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class TelegramStartTokenService {

    private static final int TRUNCATED_HMAC_BYTES = 10;
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final byte[] secret;

    public TelegramStartTokenService(@Value("${token.signing.key}") String base64Key) {
        this.secret = Base64.getDecoder().decode(base64Key);
    }

    public String sign(long userId, Duration ttl) {
        long exp = Instant.now().plus(ttl).getEpochSecond();
        String payload = userId + ":" + exp;
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        byte[] sig = hmac(payloadBytes);
        byte[] truncated = new byte[TRUNCATED_HMAC_BYTES];
        System.arraycopy(sig, 0, truncated, 0, TRUNCATED_HMAC_BYTES);
        return B64.encodeToString(payloadBytes) + "_" + B64.encodeToString(truncated);
    }

    public Optional<Long> verify(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        int sep = token.indexOf('_');
        if (sep < 0) return Optional.empty();
        try {
            byte[] payloadBytes = B64D.decode(token.substring(0, sep));
            byte[] sigBytes = B64D.decode(token.substring(sep + 1));
            if (sigBytes.length != TRUNCATED_HMAC_BYTES) return Optional.empty();
            byte[] expectedSig = hmac(payloadBytes);
            byte[] expectedTrunc = new byte[TRUNCATED_HMAC_BYTES];
            System.arraycopy(expectedSig, 0, expectedTrunc, 0, TRUNCATED_HMAC_BYTES);
            if (!MessageDigest.isEqual(sigBytes, expectedTrunc)) return Optional.empty();
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            int col = payload.indexOf(':');
            if (col < 0) return Optional.empty();
            long userId = Long.parseLong(payload.substring(0, col));
            long exp = Long.parseLong(payload.substring(col + 1));
            if (Instant.now().getEpochSecond() > exp) return Optional.empty();
            return Optional.of(userId);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    private byte[] hmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC init failed", e);
        }
    }
}
