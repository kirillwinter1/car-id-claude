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
import ru.car.dto.mobile.MobileRq;
import ru.car.dto.mobile.MobileRs;
import ru.car.dto.mobile.responseTypes.UserDtoRs;
import ru.car.exception.ErrorMessage;
import ru.car.service.UserService;
import ru.car.service.security.AuthService;

@Slf4j
@SecurityRequirements()
@Tag(name = "UserController", description = "The User API")
@ApiResponses(@ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = ErrorMessage.class))))
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AuthService authService;

    @Operation(summary = "Get User info")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = UserDtoRs.class))))
    @PostMapping("user.get")
    public ResponseEntity<MobileRs<?>> getUser(@RequestBody MobileRq<?> request) {
        log.debug("принят запрос на получение сведений о пользователе {}", authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(userService.getUser(authService.getUserId()))
                .build());
    }

    @Operation(summary = "Delete User")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = UserDtoRs.class))))
    @PostMapping("user.delete")
    public ResponseEntity<MobileRs<?>> deleteUser(@RequestBody MobileRq<?> request) {
        log.debug("принят запрос на удаление сведений о пользователе {}", authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(userService.deleteUser(authService.getUserId()))
                .build());
    }
}

