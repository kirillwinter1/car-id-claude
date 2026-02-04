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
import ru.car.dto.NotificationDto;
import ru.car.dto.NotificationUnreadPage;
import ru.car.dto.PageParam;
import ru.car.dto.mobile.MobileRq;
import ru.car.dto.mobile.MobileRs;
import ru.car.dto.mobile.requestTypes.NotificationMarkAsReadDtoRq;
import ru.car.dto.mobile.responseTypes.NotificationDtoRs;
import ru.car.dto.mobile.responseTypes.NotificationPageRs;
import ru.car.exception.ErrorMessage;
import ru.car.service.NotificationFacade;
import ru.car.service.NotificationService;
import ru.car.service.security.AuthService;
import ru.car.util.MessageUtils;

import java.util.Objects;

@Slf4j
@SecurityRequirements()
@Tag(name = "NotificationController", description = "The Notification API")
@ApiResponses(@ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = ErrorMessage.class))))
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final NotificationFacade notificationFacade;
    private final AuthService authService;

    @Operation(summary = "Get all User's Notifications")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = NotificationPageRs.class))))
    @PostMapping("notification.get_all")
    public ResponseEntity<MobileRs<?>> getAll(@RequestBody MobileRq<PageParam> request) {
        log.debug("принят запрос на получение всех нотификаций пользователем {}", authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(notificationService.getAllReceived(setDefault(request.getParams(), 0, 10)))
                .build());
    }

    @Operation(summary = "Get all Unread User's Notifications")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = NotificationUnreadPage.class))))
    @PostMapping("notification.get_all_unread")
    public ResponseEntity<MobileRs<?>> getAllUnread(@RequestBody MobileRq<PageParam> request) {
        log.debug("принят запрос на получение непрочитанных нотификаций пользователем {}", authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(notificationService.getAllUnreadReceived(setDefault(request.getParams(), 0, 10)))
                .build());
    }

    @Operation(summary = "Read Notification")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = NotificationDtoRs.class))))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            schema=@Schema(implementation = NotificationMarkAsReadDtoRq.class)))
    @PostMapping("notification.mark_as_read")
    public ResponseEntity<MobileRs<?>> readNotification(@RequestBody MobileRq<NotificationDto> request) {
        log.debug("принят запрос на прочтение нотификации {} пользователем {}", MessageUtils.getOrNull(() -> request.getParams().getNotificationId()), authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(notificationFacade.read(request.getParams()))
                .build());
    }

    private PageParam setDefault(PageParam param, Integer defPage, Integer defSize) {
        if (Objects.isNull(param.getPage()) || param.getPage() < 0) {
            param.setPage(defPage);
        }
        if (Objects.isNull(param.getSize()) || param.getSize() < 1) {
            param.setSize(defSize);
        }
        return param;
    }
}

