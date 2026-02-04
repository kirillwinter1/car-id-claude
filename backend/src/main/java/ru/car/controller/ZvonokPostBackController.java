package ru.car.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.car.dto.NotificationDto;
import ru.car.service.AuthenticationCodeService;
import ru.car.service.NotificationService;

import java.util.Objects;

@Slf4j
@Tag(name = "ZvonokPostBackController", description = "The ZvonokPostBackController API")
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class ZvonokPostBackController {

    @Value("${zvonok.codeCallCampaignId}")
    private String codeCallCampaignId;
    @Value("${zvonok.callCampaignId}")
    private String callCampaignId;
    private final AuthenticationCodeService authenticationCodeService;
    private final NotificationService notificationService;

//    GET http://gachi-huyachi.fun/api/zvonok/success?phoneFrom={ct_caller}&phoneTo={ct_phone9}&callId={ct_call_id}&campaignId={ct_campaign_id}
    @Operation(summary = "Get Zvonok Code Success PostBack")
    @GetMapping("zvonok/success")
    public ResponseEntity<?> getSuccessCall(@RequestParam String phoneFrom,
                                            @RequestParam String phoneTo,
                                            @RequestParam Long callId,
                                            @RequestParam String campaignId) {
        if (codeCallCampaignId.equals(campaignId)) {
            log.debug("принят запрос от zvonok об успешном дозвоне от {} к {}", phoneFrom, phoneTo);
            authenticationCodeService.createCodeFromCallerNumber(phoneFrom, phoneTo);
        }
        return ResponseEntity.ok("");
    }

//    GET http://gachi-huyachi.fun/api/zvonok/fail?phoneFrom={ct_caller}&phoneTo={ct_phone9}&callId={ct_call_id}&campaignId={ct_campaign_id}
    @Operation(summary = "Get Zvonok Code Fail PostBack")
    @GetMapping("zvonok/fail")
    public ResponseEntity<?> getFailCall(@RequestParam String phoneFrom,
                                         @RequestParam String phoneTo,
                                         @RequestParam Long callId,
                                         @RequestParam String campaignId) {
        if (codeCallCampaignId.equals(campaignId)) {
            log.debug("принят запрос от zvonok о неуспешном дозвоне от {} к {}", phoneFrom, phoneTo);
            authenticationCodeService.createCodeFromCallerNumber(phoneFrom, phoneTo);
        }
        return ResponseEntity.ok("");
    }

//    GET http://gachi-huyachi.fun/api/zvonok/notification/markAsRead?phoneFrom={ct_caller}&phoneTo={ct_phone9}&callId={ct_call_id}&campaignId={ct_campaign_id}
    @Operation(summary = "Get Zvonok Message Success PostBack")
    @GetMapping("zvonok/notification/markAsRead")
    public ResponseEntity<?> getNotificationMarkAsRead(@RequestParam String phoneFrom,
                                         @RequestParam String phoneTo,
                                         @RequestParam Long callId,
                                         @RequestParam String campaignId) {
        if (callCampaignId.equals(campaignId)) {
            log.debug("принят запрос от zvonok о получении уведомления c call_id {} к {}", callId, phoneTo);
            NotificationDto dto = notificationService.readByCallId(callId);
            if (Objects.isNull(dto)) {
                log.error("уведомление zvonok к {} с call_id {}  не существует", phoneTo, callId);
            } else {
                log.debug("уведомление {} с call_id {} от zvonok успешно подтверждено", dto.getNotificationId(), callId);
            }
        }
        return ResponseEntity.ok("");
    }

}
