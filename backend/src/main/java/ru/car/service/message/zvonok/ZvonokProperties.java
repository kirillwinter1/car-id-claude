package ru.car.service.message.zvonok;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties("zvonok")
public class ZvonokProperties {
    private String urlFlashcall;
    private String urlCall;
    private String flashcallCampaignId;
    private String codeCallCampaignId;
    private String callCampaignId;
    private String publicKeyApi;
}
