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
import org.springframework.web.bind.annotation.*;
import ru.car.dto.QrDto;
import ru.car.dto.mobile.MobileRq;
import ru.car.dto.mobile.MobileRs;
import ru.car.dto.mobile.requestTypes.QrIdDtoRq;
import ru.car.dto.mobile.requestTypes.QrLinkToUserDtoRq;
import ru.car.dto.mobile.responseTypes.QrDtoRs;
import ru.car.dto.mobile.responseTypes.QrDtosRs;
import ru.car.exception.ErrorMessage;
import ru.car.service.QrService;
import ru.car.service.security.AuthService;
import ru.car.util.MessageUtils;

@Slf4j
@SecurityRequirements()
@Tag(name = "QrController", description = "The Qr API")
@ApiResponses(@ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = ErrorMessage.class))))
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class QrController {
    private final QrService qrService;
    private final AuthService authService;

    @Operation(summary = "Get Qr info")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = QrDtoRs.class))))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            schema=@Schema(implementation = QrIdDtoRq.class)))
    @PostMapping("qr.get")
    public ResponseEntity<MobileRs<?>> getQr(@RequestBody MobileRq<QrDto> request) {
        log.debug("принят запрос на получение сведений о qr {} пользователем {}", MessageUtils.getOrNull(() -> request.getParams().getQrId()), authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(qrService.getQrById(request.getParams()))
                .build());
    }

    @Operation(summary = "Create new Qr")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = QrDtoRs.class))))
    @PostMapping("qr.create")
    public ResponseEntity<MobileRs<?>> createQr(@RequestBody MobileRq<?> request) {
        log.debug("принят запрос на создание qr пользователем {}", authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(qrService.createQr())
                .build());
    }

    @Operation(summary = "Create new Temporary Qr")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = QrDtoRs.class))))
    @PostMapping("qr.create_temporary")
    public ResponseEntity<MobileRs<?>> createTemporaryQr(@RequestBody MobileRq<?> request) {
        log.debug("принят запрос на создание временного qr пользователем {}", authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(qrService.createTemporaryQr(authService.getUserId()))
                .build());
    }

    @Operation(summary = "Link Qr to User")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = QrDtoRs.class))))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            schema=@Schema(implementation = QrLinkToUserDtoRq.class)))
    @PostMapping("qr.link_to_user")
    public ResponseEntity<MobileRs<?>> linkToUser(@RequestBody MobileRq<QrDto> request) {
        log.debug("принят запрос на привязке qr {} пользователем {}", MessageUtils.getOrNull(() -> request.getParams().getQrId()) , authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(qrService.linkToUser(request.getParams()))
                .build());
    }

    @Operation(summary = "Get all User's Qr")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = QrDtosRs.class))))
    @PostMapping("qr.get_all")
    public ResponseEntity<MobileRs<?>> getAll(@RequestBody MobileRq<?> request) {
        log.debug("принят запрос на получение сведений о всех qr пользователем {}", authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(qrService.getAll())
                .build());
    }

    @Operation(summary = "Delete Qr")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = QrDtoRs.class))))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            schema=@Schema(implementation = QrIdDtoRq.class)))
    @PostMapping("qr.delete")
    public ResponseEntity<MobileRs<?>> delete(@RequestBody MobileRq<QrDto> request) {
        log.debug("принят запрос на удаление qr {} пользователем {}", MessageUtils.getOrNull(() -> request.getParams().getQrId()) , authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(qrService.delete(request.getParams()))
                .build());
    }
}

