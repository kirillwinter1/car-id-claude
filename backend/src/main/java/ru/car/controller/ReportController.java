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
import ru.car.dto.mobile.requestTypes.NotificationSendDtoRq;
import ru.car.dto.mobile.responseTypes.NotificationDtoRs;
import ru.car.dto.mobile.responseTypes.NotificationPageRs;
import ru.car.dto.mobile.responseTypes.ReasonsDictionaryDtosRs;
import ru.car.service.NotificationFacade;
import ru.car.service.NotificationService;
import ru.car.service.ReasonDictionaryService;
import ru.car.service.security.AuthService;
import ru.car.util.MessageUtils;

import java.util.Objects;

@Slf4j
@SecurityRequirements()
@Tag(name = "ReportController", description = "The Report API")
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class ReportController {
    private final ReasonDictionaryService reasonDictionaryService;
    private final NotificationFacade notificationFacade;
    private final NotificationService notificationService;
    private final AuthService authService;


    @Operation(summary = "Notification Status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = NotificationDtoRs.class)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            schema=@Schema(implementation = NotificationMarkAsReadDtoRq.class)))
    @PostMapping("report.get")
    public ResponseEntity<MobileRs<?>> getNotificationSend(@RequestBody MobileRq<NotificationDto> request) {
        log.debug("принят запрос на получение отправленного уведомления {} пользователем {}", MessageUtils.getOrNull(() -> request.getParams().getNotificationId()), authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(notificationService.findByIdAndSender(request.getParams()))
                .build());
    }

    @Operation(summary = "Get all User's Notifications")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = NotificationPageRs.class))))
    @PostMapping("report.get_all")
    public ResponseEntity<MobileRs<?>> getAll(@RequestBody MobileRq<PageParam> request) {
        log.debug("принят запрос на получение отправленных уведомлений пользователем {}", authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(notificationService.getAllSend(setDefault(request.getParams(), 0, 10)))
                .build());
    }

    @Operation(summary = "Get all Unread User's Notifications")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = NotificationUnreadPage.class))))
    @PostMapping("report.get_all_unread")
    public ResponseEntity<MobileRs<?>> getAllUnread(@RequestBody MobileRq<PageParam> request) {
        log.debug("принят запрос на получение отправленных непрочитанных уведомлений пользователем {}", authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(notificationService.getAllUnreadSend(setDefault(request.getParams(), 0, 10)))
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

    @Operation(summary = "Get all Reasons for Mobile")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ReasonsDictionaryDtosRs.class))))
    @PostMapping("report.get_all_reasons")
    public ResponseEntity<MobileRs<?>> getAllReasons(@RequestBody MobileRq<?> request) {
        log.debug("принят запрос на получение списка причин пользователем {}", authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(reasonDictionaryService.findAll())
                .build());
    }

    @Operation(summary = "Send User's Notifications Mobile")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = NotificationDtoRs.class))))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            schema=@Schema(implementation = NotificationSendDtoRq.class)))
    @PostMapping("report.send")
    public ResponseEntity<MobileRs<?>> sendNotification(@RequestBody MobileRq<NotificationDto> request) {
        log.debug("принят запрос на отправку уведомления на qr {} пользователем {}", MessageUtils.getOrNull(() -> request.getParams().getQrId()), authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(notificationFacade.send(request.getParams()))
                .build());
    }
}

