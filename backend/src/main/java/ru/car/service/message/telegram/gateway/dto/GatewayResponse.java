package ru.car.service.message.telegram.gateway.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GatewayResponse {
    private boolean ok;
    private String error;
    private GatewayResult result;
}
