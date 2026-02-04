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
import ru.car.dto.QrDto;
import ru.car.exception.ErrorMessage;
import ru.car.service.QrService;

import java.util.UUID;

@Slf4j
@Tag(name = "QrWebController", description = "The Qr API")
@ApiResponses(@ApiResponse(responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = ErrorMessage.class))))
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class QrWebController {
    private final QrService qrService;

    @Operation(summary = "Get Qr info Web")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = QrDto.class))))
    @GetMapping("qr/{id}")
    public ResponseEntity<QrDto> getQrForWeb(@PathVariable UUID id) {
        QrDto dto = QrDto.builder()
                .qrId(id)
                .build();
        log.debug("принят запрос на получение сведений qr {} анонимным пользователем", id);
        return ResponseEntity.ok(qrService.getQrById(dto));
    }
}

