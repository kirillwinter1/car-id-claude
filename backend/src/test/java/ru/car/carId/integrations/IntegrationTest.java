package ru.car.carId.integrations;

import com.codeborne.selenide.ClickOptions;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ru.car.dto.NotificationDto;
import ru.car.dto.QrDto;
import ru.car.dto.login_oauth_code.LoginAuthCodeRqParams;
import ru.car.dto.mobile.MobileRq;
import ru.car.dto.mobile.MobileRs;
import ru.car.dto.mobile.responseTypes.LoginAuthCodeRs;

import java.util.Map;
import java.util.UUID;

public class IntegrationTest {
    private String url = "http://gachi-huyachi.fun";
    private RestTemplate restTemplate = new RestTemplate();

//    @Test
    void openBrowser() {

        // авторизуемся под админом
        String token = getAuthToken();

        // создаем qr
        Map<String, Object> qr = createQr(token);
        Assertions.assertNotNull(qr);
        Assertions.assertNotNull(qr.get("qr_id"));

        // привязываем qr к себе
        UUID id = UUID.fromString((String) qr.get("qr_id"));
        System.out.println("Создан и привязан к админу QR " + id);
        qr = linkQr(token, id);
        Assertions.assertNotNull(qr);

        // отправляем на него уведомление через браузер
        Selenide.open("http://gachi-huyachi.fun/qr/" + id);
        Selenide.$(By.xpath("//*[@id=\"radio-btns\"]/label[1]")).click(ClickOptions.usingDefaultMethod());
        Selenide.$(By.xpath("//*[@id=\"submit\"]")).click(ClickOptions.usingDefaultMethod());
        // нажимаем кнопку понятно, нас перекидывает на страницу уведомления
//        Selenide.$(By.xpath("//*[@id=\"close-btn\"]")).click(ClickOptions.usingDefaultMethod());

        // получаем уведомление
        UUID notificationId = getNotificationIdFromUrl();
        System.out.println("Id уведомления " + notificationId);
        String h2 = Selenide.$(By.xpath("//*[@id=\"send-msg\"]/div/h2")).text();
        Assertions.assertEquals(h2, "Сообщение доставлено");

        // отмечаем его прочитанным
        readNotification(token, notificationId);
        Selenide.sleep(5_000);

        // проверяем страницу браузера
        h2 = Selenide.$(By.xpath("//*[@id=\"send-msg\"]/div/h2")).text();
        Assertions.assertEquals(h2, "Сообщение прочитано");
//        Selenide.sleep(600_000);

    }

    String getAuthToken() {
        MobileRq<Object> request = MobileRq.builder()
                .method("user.login_oauth_code")
                .params(LoginAuthCodeRqParams.builder()
                        .phoneNumber("79008007766")
                        .code("1111")
                        .build())
                .build();

        ResponseEntity<LoginAuthCodeRs> mobileRsResponseEntity = restTemplate.postForEntity(url, request, LoginAuthCodeRs.class);
        return mobileRsResponseEntity.getBody().getParams().getToken();
    }

    Map<String, Object> createQr(String token) {
        MobileRq<Object> request = MobileRq.builder()
                .method("qr.create")
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(token);
        HttpEntity<MobileRq<Object>> entity = new HttpEntity<>(request, httpHeaders);

        ResponseEntity<MobileRs> response = restTemplate.exchange(url, HttpMethod.POST, entity, MobileRs.class);
        return (Map<String, Object>) response.getBody().getParams();
    }

    Map<String, Object> linkQr(String token, UUID qrId) {
        MobileRq<Object> request = MobileRq.builder()
                .method("qr.link_to_user")
                .params(QrDto.builder()
                        .qrId(qrId)
                        .qrName("name")
                        .build())
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(token);
        HttpEntity<MobileRq<Object>> entity = new HttpEntity<>(request, httpHeaders);

        ResponseEntity<MobileRs> response = restTemplate.exchange(url, HttpMethod.POST, entity, MobileRs.class);
        return (Map<String, Object>) response.getBody().getParams();
    }

    UUID getNotificationIdFromUrl() {
        String currentUrl = WebDriverRunner.getWebDriver().getCurrentUrl();
        String[] split = currentUrl.split("/");
        return UUID.fromString(split[split.length - 1]);
    }

    Map<String, Object> readNotification(String token, UUID notificationId) {
        MobileRq<Object> request = MobileRq.builder()
                .method("notification.mark_as_read")
                .params(NotificationDto.builder()
                        .notificationId(notificationId)
                        .build())
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(token);
        HttpEntity<MobileRq<Object>> entity = new HttpEntity<>(request, httpHeaders);

        ResponseEntity<MobileRs> response = restTemplate.exchange(url, HttpMethod.POST, entity, MobileRs.class);
        return (Map<String, Object>) response.getBody().getParams();
    }
}
