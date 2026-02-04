package ru.car.service.message.firebase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties("firebase")
public class FirebaseProperties {
    private List<String> scopeCredentials;
    private String sourceCredentials;
    private String url;
}
