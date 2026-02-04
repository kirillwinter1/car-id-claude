package ru.car.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.car.dto.FirebaseTokenDto;
import ru.car.dto.NotificationSettingDto;
import ru.car.dto.mobile.MobileRq;
import ru.car.dto.mobile.MobileRs;
import ru.car.dto.mobile.responseTypes.NotificationSettingDtoRs;
import ru.car.service.FirebaseTokenService;
import ru.car.service.NotificationSettingService;
import ru.car.service.security.AuthService;

@Slf4j
@SecurityRequirements()
@Tag(name = "NotificationSettingController", description = "The Notification Setting API")
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class NotificationSettingController {
    private final NotificationSettingService notificationSettingService;
    private final FirebaseTokenService firebaseTokenService;
    private final AuthService authService;

    @Operation(summary = "Get Notification Setting info")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = NotificationSettingDtoRs.class))))
    @PostMapping("notification_settings.get")
    public ResponseEntity<MobileRs<?>> getNotificationSetting(@RequestBody MobileRq<?> request) {
        log.debug("принят запрос на получение настроек пользователем {}", authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(notificationSettingService.get(authService.getUserId()))
                .build());
    }

    @Operation(summary = "Patch Notification Setting info")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = NotificationSettingDtoRs.class))))
    @PostMapping("notification_settings.patch")
    public ResponseEntity<MobileRs<?>> patchNotificationSetting(@RequestBody MobileRq<NotificationSettingDto> request) {
        log.debug("принят запрос на изменение настроек пользователем {}", authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(notificationSettingService.patch(authService.getUserId(), request.getParams()))
                .build());
    }

    @Operation(summary = "Update Notification Setting token")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = FirebaseTokenDto.class))))
    @PostMapping("notification_settings.update_token")
    public ResponseEntity<MobileRs<?>> updateNotificationSettingToken(@RequestBody MobileRq<FirebaseTokenDto> request) {
        log.debug("принят запрос на обновление токена firebase пользователем {}", authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(firebaseTokenService.createOrUpdateToken(request.getParams()))
                .build());
    }
}
