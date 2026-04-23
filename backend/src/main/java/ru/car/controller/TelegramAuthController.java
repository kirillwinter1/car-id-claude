package ru.car.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.car.dto.TelegramStartUrlDto;
import ru.car.dto.mobile.MobileRs;
import ru.car.service.message.telegram.TelegramProperties;
import ru.car.service.message.telegram.auth.TelegramStartTokenService;
import ru.car.service.security.AuthService;

import java.time.Duration;

@Slf4j
@Tag(name = "TelegramAuthController", description = "Telegram deep-link onboarding")
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class TelegramAuthController {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);

    private final TelegramStartTokenService tokenService;
    private final TelegramProperties tgProps;
    private final AuthService authService;

    @PostMapping("telegram.get_start_url")
    public ResponseEntity<MobileRs<TelegramStartUrlDto>> getStartUrl() {
        Long userId = authService.getUserId();
        String token = tokenService.sign(userId, TOKEN_TTL);
        String url = "https://t.me/" + tgProps.getBot() + "?start=" + token;
        MobileRs<TelegramStartUrlDto> body = MobileRs.<TelegramStartUrlDto>builder()
            .result("true")
            .params(TelegramStartUrlDto.builder().url(url).build())
            .build();
        return ResponseEntity.ok(body);
    }
}
