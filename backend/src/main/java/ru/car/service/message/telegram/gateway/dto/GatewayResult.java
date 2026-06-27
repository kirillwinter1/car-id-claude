package ru.car.service.message.telegram.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GatewayResult {
    @JsonProperty("request_id")
    private String requestId;
    @JsonProperty("remaining_balance")
    private Double remainingBalance;
    @JsonProperty("request_cost")
    private Double requestCost;
}
