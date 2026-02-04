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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.car.dto.mobile.MobileRq;
import ru.car.dto.mobile.MobileRs;
import ru.car.dto.mobile.responseTypes.VersionControlDtoRs;
import ru.car.service.VersionControlService;

@Slf4j
@Tag(name = "VersionControlController", description = "The VersionControl API")
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class VersionControlController {

    private final VersionControlService versionControlService;

    @Operation(summary = "Get VersionControl info")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = VersionControlDtoRs.class))))
    @PostMapping("version_control.get")
    public ResponseEntity<MobileRs<?>> getVersionControl(@RequestBody MobileRq<?> request) {
        log.debug("принят запрос на получение версий приложения");
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(versionControlService.get())
                .build());
    }
}
