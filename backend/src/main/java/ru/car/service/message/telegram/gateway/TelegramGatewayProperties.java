package ru.car.service.message.telegram.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties("telegram.gateway")
public class TelegramGatewayProperties {
    private Boolean enabled = false;
    private String baseUrl = "https://gatewayapi.telegram.org";
    private String accessToken;
    /** TTL кода в секундах (Gateway допускает 30..3600). */
    private Integer ttl = 300;
    /** Длина генерируемого кода (совпадает с varchar(4) authentication_code). */
    private Integer codeLength = 4;
}
