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
import ru.car.dto.mobile.responseTypes.MarketplacesDtoRs;
import ru.car.service.MarketplaceService;

@Slf4j
@Tag(name = "MarketplacesController", description = "The Marketplaces API")
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class MarketplacesController {

    private final MarketplaceService marketplaceService;

    @Operation(summary = "Get Marketplaces info")
    @ApiResponses(@ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = MarketplacesDtoRs.class))))
    @PostMapping("marketplaces.get")
    public ResponseEntity<MobileRs<?>> getMarketplace(@RequestBody MobileRq<?> request) {
        log.debug("принят запрос на получение информации о маркетплейсах");
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(marketplaceService.get())
                .build());
    }
}
