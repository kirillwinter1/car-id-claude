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
import ru.car.dto.login_auth_mobile.LoginAuthMobileRqParams;
import ru.car.dto.login_oauth_code.LoginAuthCodeRqParams;
import ru.car.dto.mobile.MobileRq;
import ru.car.dto.mobile.MobileRs;
import ru.car.dto.mobile.responseTypes.LoginAuthCodeRs;
import ru.car.dto.mobile.responseTypes.LoginAuthMobileRs;
import ru.car.exception.ErrorMessage;
import ru.car.service.LoginAuthMobileService;
import ru.car.service.security.AuthService;
import ru.car.util.MessageUtils;

@Slf4j
@Tag(name = "LoginAuthMobileController", description = "The Login API")
@ApiResponses(@ApiResponse(responseCode = "403", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = ErrorMessage.class))))
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class LoginAuthMobileController {
    private final LoginAuthMobileService loginAuthMobileService;
    private final AuthService authService;

    @Operation(summary = "Request for code call")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = LoginAuthMobileRs.class))))
    @PostMapping("user.login_oauth_mobile")
    public ResponseEntity<MobileRs<?>> sendCode(@RequestBody MobileRq<LoginAuthMobileRqParams> request) {
        log.debug("принят запрос на отправку кода на телефон {}", MessageUtils.getOrNull(() -> request.getParams().getPhoneNumber()));
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(loginAuthMobileService.loginMobile(request.getParams()))
                .build());
    }

    @Operation(summary = "Send code for auth")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = LoginAuthCodeRs.class))))
    @PostMapping("user.login_oauth_code")
    public ResponseEntity<MobileRs<?>> confirmCode(@RequestBody MobileRq<LoginAuthCodeRqParams> request) {
        log.debug("принят запрос на подтверждение кода на телефон {}", MessageUtils.getOrNull(request::getParams));
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(loginAuthMobileService.confirmCode(request.getParams()))
                .build());
    }

    @SecurityRequirements
    @Operation(summary = "Request for logout")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = MobileRs.class))))
    @PostMapping("user.logout")
    public ResponseEntity<MobileRs<?>> logout(@RequestBody MobileRq<?> request) {
        log.debug("принят запрос на логаут пользователя {}", authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(loginAuthMobileService.logout())
                .build());
    }
}
