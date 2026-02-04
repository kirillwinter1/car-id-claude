package ru.car.service.message.whatsapp;

import com.greenapi.pkg.api.GreenApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WhatsappProperties.class)
public class WhatsappConfig {

    @Bean
    public GreenApi greenApi(WhatsappProperties properties) {
        return new GreenApi(
                new RestTemplateBuilder().build(),
                properties.getHostMedia(),
                properties.getHost(),
                properties.getInstanceId(),
                properties.getToken());
    }
}
