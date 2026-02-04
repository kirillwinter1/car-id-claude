package ru.car.service.message.mail;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import ru.car.model.NotificationSetting;
import ru.car.service.message.Sender;
import ru.car.service.message.TextMessage;

import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(MailProperties.class)
public class MailSender implements Sender {
    private final MailProperties properties;

    public boolean sendMessage(String mailFrom, String textToSend) {
        Properties props = new Properties();
        props.put("mail.host", properties.getSmtpHost());
        props.put("mail.smtp.host", properties.getSmtpHost());
        props.put("mail.smtp.port", 587);
        props.put("mail.smtp.auth", "true");
//        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.transport.protocol", "smtps");
//        props.put("mail.smtp.sendpartial", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.username", properties.getSenderMail());
        props.put("mail.smtp.password", properties.getPassword());
        props.put("mail.smtps.timeout", "1000");
        props.put("mail.smtps.connectiontimeout", "1000");




        Session session = Session.getDefaultInstance(props,
                new jakarta.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(properties.getSenderMail(), properties.getPassword());
                    }
                });
//        session.setDebug(true);

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(properties.getSenderMail()));
            message.addRecipient(Message.RecipientType.TO,new InternetAddress(properties.getReceiverMail()));
            message.setSubject("feedback from " + mailFrom);
            message.setText(textToSend);

            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getServiceName() {
        return "mail";
    }

    @Override
    public boolean sendNotification(TextMessage message) {
        return sendMessage(message.getMail(), message.getText()) ;
    }

    @Override
    public boolean canSendNotification(NotificationSetting setting) {
        return true;
    }
}
