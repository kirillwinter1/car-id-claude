package ru.car.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.car.dto.NotificationStatusDto;
import ru.car.exception.ErrorMessage;
import ru.car.service.NotificationService;

import java.util.UUID;

@Slf4j
@Tag(name = "NotificationWebController", description = "The Notification Web API")
@ApiResponses(@ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = ErrorMessage.class))))
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class NotificationWebController {
    private final NotificationService notificationService;

    @Operation(summary = "Notification Status Web")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = NotificationStatusDto.class)))
    })
    @GetMapping("notification/{id}/status")
    public ResponseEntity<NotificationStatusDto> getNotificationStatus(@PathVariable UUID id) {
        log.debug("принят запрос на получение статуса уведомления {}", id);
        return ResponseEntity.ok(notificationService.getStatus(id));
    }
}

