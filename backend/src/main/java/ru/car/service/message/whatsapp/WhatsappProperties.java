package ru.car.service.message.whatsapp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties("green-api")
public class WhatsappProperties {
    private String host;
    private String hostMedia;
    private String instanceId;
    private String token;
    private Boolean enable;
}
