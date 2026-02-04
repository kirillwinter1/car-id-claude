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
import ru.car.dto.FeedbackDto;
import ru.car.dto.mobile.MobileRq;
import ru.car.dto.mobile.MobileRs;
import ru.car.dto.mobile.requestTypes.FeedbackDtoRq;
import ru.car.dto.mobile.responseTypes.FeedbackDtoRs;
import ru.car.dto.web.requestTypes.FeedbackDtoRqWeb;
import ru.car.exception.ErrorMessage;
import ru.car.service.FeedbackFacade;
import ru.car.service.security.AuthService;
import ru.car.util.MessageUtils;

@Slf4j
@Tag(name = "FeedbackController", description = "The Feedback API")
@ApiResponses(@ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = ErrorMessage.class))))
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class FeedbackController {
    private final FeedbackFacade feedbackFacade;
    private final AuthService authService;

    @SecurityRequirements()
    @Operation(summary = "Send User's Feedback")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = FeedbackDtoRs.class))))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            schema=@Schema(implementation = FeedbackDtoRq.class)))
    @PostMapping("feedback.send")
    public ResponseEntity<MobileRs<?>> sendFeedback(@RequestBody MobileRq<FeedbackDto> request) {
        log.debug("принят запрос на федбек на {} пользователем {}", MessageUtils.getOrNull(() -> request.getParams().getEmail()), authService.getUserId());
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(feedbackFacade.send(request.getParams()))
                .build());
    }

    @Operation(summary = "Send User's Feedback Web")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = FeedbackDto.class))))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            schema=@Schema(implementation = FeedbackDtoRqWeb.class)))
    @PostMapping("feedback/send")
    public ResponseEntity<?> sendFeedbackWeb(@RequestBody FeedbackDto request) {
        log.debug("принят запрос на федбек на {}", MessageUtils.getOrNull(request::getEmail));
        return ResponseEntity.ok(feedbackFacade.sendAnonymous(request));
    }
}
