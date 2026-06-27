package ru.car.service.vk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties("vk")
public class VkIdProperties {
    private Boolean enabled = false;
    private String clientId;
    private String userInfoUrl = "https://id.vk.ru/oauth2/user_info";
}
