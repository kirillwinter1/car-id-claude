package ru.car.service.message.mail;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties("mail")
public class MailProperties {
    private String receiverMail;
    private String senderMail;
    private String password;
    private String smtpHost;
}
