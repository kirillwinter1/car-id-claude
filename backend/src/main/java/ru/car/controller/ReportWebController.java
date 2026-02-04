package ru.car.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.car.dto.NotificationDto;
import ru.car.dto.ReasonDictionaryDto;
import ru.car.dto.web.requestTypes.NotificationSendDtoRqWeb;
import ru.car.dto.web.requestTypes.NotificationUpdateDraftDtoRqWeb;
import ru.car.service.NotificationFacade;
import ru.car.service.NotificationService;
import ru.car.service.ReasonDictionaryService;
import ru.car.util.MessageUtils;

import java.util.List;

@Slf4j
@Tag(name = "ReportWebController", description = "The Report Web API")
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class ReportWebController {
    private final ReasonDictionaryService reasonDictionaryService;
    private final NotificationFacade notificationFacade;
    private final NotificationService notificationService;


    @Operation(summary = "Get all Reasons for Web")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            array = @ArraySchema(schema = @Schema(implementation = ReasonDictionaryDto.class)))))
    @GetMapping("report/get_all_reasons")
    public ResponseEntity<List<ReasonDictionaryDto>> getAllWeb() {
        log.debug("принят запрос на получение всех причин");
        return ResponseEntity.ok(reasonDictionaryService.findAll());
    }

    @Operation(summary = "Create User's Notifications draft Web")
    @ApiResponses(@ApiResponse(responseCode = "201", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = NotificationDto.class))))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = NotificationSendDtoRqWeb.class)))
    @PostMapping("report/createDraft")
    public ResponseEntity<NotificationDto> createDraftNotificationWeb(@RequestBody NotificationDto request) {
        log.debug("принят запрос на создание черновика уведомления на qr {}", MessageUtils.getOrNull(request::getQrId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.createDraft(request));
    }

    @Operation(summary = "Update User's Notifications draft Web")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = NotificationDto.class))))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = NotificationUpdateDraftDtoRqWeb.class)))
    @PutMapping("report/updateDraft")
    public ResponseEntity<NotificationDto> updateDraftNotificationWeb(@RequestBody NotificationDto request) {
        log.debug("принят запрос на изменение черновика уведомления на qr {}", MessageUtils.getOrNull(request::getNotificationId));
        return ResponseEntity.ok(notificationFacade.updateAndTrySendDraft(request));
    }

    @Operation(summary = "Send User's Notifications Web")
    @ApiResponses(@ApiResponse(responseCode = "201", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = NotificationDto.class))))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = NotificationSendDtoRqWeb.class)))
    @PostMapping("report/send")
    public ResponseEntity<NotificationDto> sendNotificationWeb(@RequestBody NotificationDto request) {
        log.debug("принят запрос на отправку уведомления на qr {}", MessageUtils.getOrNull(request::getQrId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationFacade.send(request));
    }
}

