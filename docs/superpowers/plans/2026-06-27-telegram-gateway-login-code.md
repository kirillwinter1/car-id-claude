# Telegram Gateway Login Code — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Доставлять код входа в приложение через Telegram Gateway API (по номеру телефона), с фолбэком на текущий flashcall/SMS.

**Architecture:** Режим B — код генерим и проверяем сами (как сейчас, через `authentication_code` + `user.login_oauth_code`); Telegram Gateway лишь доставляет. Новый `TelegramGatewayService` (HTTP к `https://gatewayapi.telegram.org`) ходит через тот же SOCKS5-прокси (sing-box), что и бот (домен заблокирован в РФ). Точка выбора канала — `MessageService.sendLoginCode`.

**Tech Stack:** Java 17, Spring Boot 3.2, RestTemplate (+ `SimpleClientHttpRequestFactory` с SOCKS-прокси), Lombok, JUnit5 + Mockito + `MockRestServiceServer`.

## Global Constraints

- Бэкенд на `JdbcTemplate`, не JPA. Не менять auth-проверку кода и выдачу JWT — переиспользуем существующее.
- По умолчанию `telegram.gateway.enabled=false` → поведение входа не меняется (фолбэк на текущий flashcall/SMS). Включение на проде — отдельный ops-шаг после получения access token.
- Прокси переиспользуется из `telegram.proxy.*` (`TelegramProperties.Proxy`), отдельный прокси-конфиг не заводим (DRY).
- Секрет `telegram.gateway.accessToken` — `ENC(...)` (Jasypt) в prod; в base/dev — пусто.
- Формат телефона в Gateway — E.164 со знаком `+` (как в `ZvonokService`: `"+" + phoneNumber`).
- Коммиты — частые, после каждой задачи. Запуск тестов: `cd backend && ./gradlew test`.

---

### Task 1: TelegramGatewayService.checkSendAbility (+ properties, DTO, RestTemplate с прокси)

**Files:**
- Create: `backend/src/main/java/ru/car/service/message/telegram/gateway/TelegramGatewayProperties.java`
- Create: `backend/src/main/java/ru/car/service/message/telegram/gateway/dto/GatewayResponse.java`
- Create: `backend/src/main/java/ru/car/service/message/telegram/gateway/dto/GatewayResult.java`
- Create: `backend/src/main/java/ru/car/service/message/telegram/gateway/TelegramGatewayService.java`
- Test: `backend/src/test/java/ru/car/service/message/telegram/gateway/TelegramGatewayServiceTest.java`

**Interfaces:**
- Consumes: `TelegramProperties.getProxy()` (поля `enabled/host/port` из уже существующего `TelegramProperties.Proxy`).
- Produces:
  - `TelegramGatewayService.isEnabled(): boolean`
  - `TelegramGatewayService.checkSendAbility(String phoneNumber): String` (request_id если доставится, иначе `null`)
  - `TelegramGatewayService.sendCode(String phoneNumber, String code, String requestId): boolean` (Task 2)
  - `TelegramGatewayProperties` (поля: `enabled`, `baseUrl`, `accessToken`, `ttl`, `codeLength`)

- [ ] **Step 1: Создать `TelegramGatewayProperties`**

```java
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
```

- [ ] **Step 2: Создать DTO ответа Gateway**

`GatewayResponse.java`:
```java
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
```

`GatewayResult.java`:
```java
package ru.car.service.message.telegram.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GatewayResult {
    @JsonProperty("request_id")
    private String requestId;
    @JsonProperty("remaining_balance")
    private Double remainingBalance;
    @JsonProperty("request_cost")
    private Double requestCost;
}
```

- [ ] **Step 3: Написать падающий тест `TelegramGatewayServiceTest` (checkSendAbility)**

```java
package ru.car.service.message.telegram.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.car.test.base.BaseUnitTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("TelegramGatewayService Tests")
class TelegramGatewayServiceTest extends BaseUnitTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private TelegramGatewayService service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        TelegramGatewayProperties props = new TelegramGatewayProperties(
                true, "https://gatewayapi.telegram.org", "TOKEN", 300, 4);
        service = new TelegramGatewayService(props, restTemplate);
    }

    @Test
    @DisplayName("checkSendAbility возвращает request_id при ok=true")
    void checkSendAbilityReturnsRequestId() {
        server.expect(requestTo("https://gatewayapi.telegram.org/checkSendAbility"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer TOKEN"))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"request_id\":\"req-123\"}}",
                        MediaType.APPLICATION_JSON));

        assertThat(service.checkSendAbility("79991234567")).isEqualTo("req-123");
        server.verify();
    }

    @Test
    @DisplayName("checkSendAbility возвращает null при ok=false (нет Telegram)")
    void checkSendAbilityReturnsNullWhenNotOk() {
        server.expect(requestTo("https://gatewayapi.telegram.org/checkSendAbility"))
                .andRespond(withSuccess("{\"ok\":false,\"error\":\"PHONE_NOT_TELEGRAM\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(service.checkSendAbility("79991234567")).isNull();
    }

    @Test
    @DisplayName("checkSendAbility возвращает null при HTTP-ошибке")
    void checkSendAbilityReturnsNullOnHttpError() {
        server.expect(requestTo("https://gatewayapi.telegram.org/checkSendAbility"))
                .andRespond(withServerError());

        assertThat(service.checkSendAbility("79991234567")).isNull();
    }
}
```

- [ ] **Step 4: Запустить тест — убедиться, что не компилируется/падает (нет `TelegramGatewayService`)**

Run: `cd backend && ./gradlew test --tests "ru.car.service.message.telegram.gateway.TelegramGatewayServiceTest"`
Expected: FAIL (compilation: cannot find symbol `TelegramGatewayService`).

- [ ] **Step 5: Реализовать `TelegramGatewayService` (checkSendAbility + post + RestTemplate с SOCKS-прокси)**

```java
package ru.car.service.message.telegram.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.car.service.message.telegram.TelegramProperties;
import ru.car.service.message.telegram.gateway.dto.GatewayResponse;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;

@Slf4j
@Component
@EnableConfigurationProperties(TelegramGatewayProperties.class)
public class TelegramGatewayService {

    private final TelegramGatewayProperties properties;
    private final RestTemplate restTemplate;

    @Autowired
    public TelegramGatewayService(TelegramGatewayProperties properties, TelegramProperties telegramProperties) {
        this(properties, buildRestTemplate(telegramProperties.getProxy()));
    }

    // package-private — для тестов с MockRestServiceServer
    TelegramGatewayService(TelegramGatewayProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(properties.getEnabled());
    }

    /** @return request_id, если код можно доставить в Telegram; иначе null. */
    public String checkSendAbility(String phoneNumber) {
        try {
            GatewayResponse resp = post("checkSendAbility", Map.of("phone_number", "+" + phoneNumber));
            if (resp != null && resp.isOk() && resp.getResult() != null) {
                return resp.getResult().getRequestId();
            }
            log.warn("Telegram Gateway checkSendAbility не ok для {}: {}",
                    phoneNumber, resp == null ? "null" : resp.getError());
        } catch (Exception e) {
            log.warn("Telegram Gateway checkSendAbility ошибка для {}: {}", phoneNumber, e.getMessage());
        }
        return null;
    }

    private GatewayResponse post(String method, Map<String, String> params) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(properties.getAccessToken());
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        params.forEach(body::add);
        ResponseEntity<GatewayResponse> resp = restTemplate.exchange(
                properties.getBaseUrl() + "/" + method, HttpMethod.POST,
                new HttpEntity<>(body, headers), GatewayResponse.class);
        return resp.getBody();
    }

    private static RestTemplate buildRestTemplate(TelegramProperties.Proxy proxy) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(20000);
        if (proxy != null && proxy.isEnabled()) {
            factory.setProxy(new Proxy(Proxy.Type.SOCKS,
                    new InetSocketAddress(proxy.getHost(), proxy.getPort())));
        }
        return new RestTemplate(factory);
    }
}
```

- [ ] **Step 6: Запустить тест — убедиться, что проходит**

Run: `cd backend && ./gradlew test --tests "ru.car.service.message.telegram.gateway.TelegramGatewayServiceTest"`
Expected: PASS (3 теста).

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/ru/car/service/message/telegram/gateway/ backend/src/test/java/ru/car/service/message/telegram/gateway/
git commit -m "feat(telegram-gateway): checkSendAbility + клиент через SOCKS5-прокси"
```

---

### Task 2: TelegramGatewayService.sendCode

**Files:**
- Modify: `backend/src/main/java/ru/car/service/message/telegram/gateway/TelegramGatewayService.java`
- Test: `backend/src/test/java/ru/car/service/message/telegram/gateway/TelegramGatewayServiceTest.java`

**Interfaces:**
- Consumes: `post(...)` helper из Task 1.
- Produces: `sendCode(String phoneNumber, String code, String requestId): boolean`.

- [ ] **Step 1: Добавить падающие тесты в `TelegramGatewayServiceTest`**

```java
    @Test
    @DisplayName("sendCode возвращает true при ok=true и шлёт code/ttl/request_id")
    void sendCodeReturnsTrue() {
        server.expect(requestTo("https://gatewayapi.telegram.org/sendVerificationMessage"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer TOKEN"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .content().formData(formData()))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"request_id\":\"req-123\"}}",
                        MediaType.APPLICATION_JSON));

        assertThat(service.sendCode("79991234567", "1234", "req-123")).isTrue();
        server.verify();
    }

    private static org.springframework.util.MultiValueMap<String, String> formData() {
        org.springframework.util.MultiValueMap<String, String> m =
                new org.springframework.util.LinkedMultiValueMap<>();
        m.add("phone_number", "+79991234567");
        m.add("code", "1234");
        m.add("ttl", "300");
        m.add("request_id", "req-123");
        return m;
    }

    @Test
    @DisplayName("sendCode возвращает false при ok=false")
    void sendCodeReturnsFalseWhenNotOk() {
        server.expect(requestTo("https://gatewayapi.telegram.org/sendVerificationMessage"))
                .andRespond(withSuccess("{\"ok\":false,\"error\":\"BALANCE_NOT_ENOUGH\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(service.sendCode("79991234567", "1234", "req-123")).isFalse();
    }

    @Test
    @DisplayName("sendCode возвращает false при HTTP-ошибке")
    void sendCodeReturnsFalseOnHttpError() {
        server.expect(requestTo("https://gatewayapi.telegram.org/sendVerificationMessage"))
                .andRespond(withServerError());

        assertThat(service.sendCode("79991234567", "1234", "req-123")).isFalse();
    }
```

- [ ] **Step 2: Запустить — убедиться, что падает**

Run: `cd backend && ./gradlew test --tests "ru.car.service.message.telegram.gateway.TelegramGatewayServiceTest"`
Expected: FAIL (cannot find symbol `sendCode`).

- [ ] **Step 3: Реализовать `sendCode`** (добавить метод в `TelegramGatewayService`)

```java
    /** Доставляет наш код через Telegram. @return true, если Gateway принял (ok=true). */
    public boolean sendCode(String phoneNumber, String code, String requestId) {
        try {
            java.util.Map<String, String> params = new java.util.HashMap<>();
            params.put("phone_number", "+" + phoneNumber);
            params.put("code", code);
            params.put("ttl", String.valueOf(properties.getTtl()));
            if (requestId != null) {
                params.put("request_id", requestId);
            }
            GatewayResponse resp = post("sendVerificationMessage", params);
            boolean ok = resp != null && resp.isOk();
            if (!ok) {
                log.warn("Telegram Gateway sendVerificationMessage не ok для {}: {}",
                        phoneNumber, resp == null ? "null" : resp.getError());
            }
            return ok;
        } catch (Exception e) {
            log.warn("Telegram Gateway sendVerificationMessage ошибка для {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }
```

- [ ] **Step 4: Запустить — убедиться, что проходит**

Run: `cd backend && ./gradlew test --tests "ru.car.service.message.telegram.gateway.TelegramGatewayServiceTest"`
Expected: PASS (6 тестов).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/ru/car/service/message/telegram/gateway/TelegramGatewayService.java backend/src/test/java/ru/car/service/message/telegram/gateway/TelegramGatewayServiceTest.java
git commit -m "feat(telegram-gateway): sendVerificationMessage (доставка кода)"
```

---

### Task 3: MessageService.sendLoginCode (выбор канала Telegram→fallback)

**Files:**
- Modify: `backend/src/main/java/ru/car/service/message/MessageService.java`
- Test: `backend/src/test/java/ru/car/service/message/MessageServiceLoginCodeTest.java` (Create)

**Interfaces:**
- Consumes: `TelegramGatewayService.isEnabled()/checkSendAbility(...)/sendCode(...)`, существующий `ZvonokService.sendCode(...)`.
- Produces:
  - `MessageService.LoginCodeResult` — `record LoginCodeResult(String code, String channel)`
  - `MessageService.sendLoginCode(String telephone): LoginCodeResult`

- [ ] **Step 1: Написать падающий тест `MessageServiceLoginCodeTest`**

```java
package ru.car.service.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.car.service.AuthenticationCodeService;
import ru.car.service.message.telegram.gateway.TelegramGatewayService;
import ru.car.service.message.zvonok.ZvonokService;
import ru.car.test.base.BaseUnitTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("MessageService.sendLoginCode Tests")
class MessageServiceLoginCodeTest extends BaseUnitTest {

    @Mock private TelegramGatewayService telegramGatewayService;
    @Mock private ZvonokService zvonokService;
    @Mock private AuthenticationCodeService authenticationCodeService;

    @InjectMocks private MessageService messageService;

    @Test
    @DisplayName("используем Telegram, когда код доставим")
    void usesTelegramWhenDeliverable() {
        when(telegramGatewayService.isEnabled()).thenReturn(true);
        when(telegramGatewayService.checkSendAbility("79991234567")).thenReturn("req-1");
        when(telegramGatewayService.sendCode(eq("79991234567"), anyString(), eq("req-1"))).thenReturn(true);

        MessageService.LoginCodeResult result = messageService.sendLoginCode("79991234567");

        assertThat(result.channel()).isEqualTo("telegram");
        assertThat(result.code()).hasSize(4);
        verify(zvonokService, never()).sendCode(any());
    }

    @Test
    @DisplayName("фолбэк на flashcall, когда Telegram не доставит")
    void fallsBackWhenNotDeliverable() {
        when(telegramGatewayService.isEnabled()).thenReturn(true);
        when(telegramGatewayService.checkSendAbility(anyString())).thenReturn(null);
        when(zvonokService.sendCode("79991234567")).thenReturn("4321");

        MessageService.LoginCodeResult result = messageService.sendLoginCode("79991234567");

        assertThat(result.channel()).isEqualTo("call");
        assertThat(result.code()).isEqualTo("4321");
    }

    @Test
    @DisplayName("фолбэк, когда Gateway выключен (checkSendAbility не зовём)")
    void fallsBackWhenGatewayDisabled() {
        when(telegramGatewayService.isEnabled()).thenReturn(false);
        when(zvonokService.sendCode("79991234567")).thenReturn("4321");

        MessageService.LoginCodeResult result = messageService.sendLoginCode("79991234567");

        assertThat(result.channel()).isEqualTo("call");
        verify(telegramGatewayService, never()).checkSendAbility(anyString());
    }
}
```

- [ ] **Step 2: Запустить — убедиться, что падает**

Run: `cd backend && ./gradlew test --tests "ru.car.service.message.MessageServiceLoginCodeTest"`
Expected: FAIL (cannot find symbol `TelegramGatewayService` field / `LoginCodeResult` / `sendLoginCode`).

- [ ] **Step 3: Реализовать в `MessageService`**

3a. Добавить импорт и поле (в блок зависимостей, рядом с `zvonokService`):
```java
import ru.car.service.message.telegram.gateway.TelegramGatewayService;
```
```java
    private final TelegramGatewayService telegramGatewayService;
```

3b. Добавить record и метод (после `sendFlashcallCode`, перед `sendCallCode`):
```java
    public record LoginCodeResult(String code, String channel) {}

    /** Код входа: сперва пробуем Telegram Gateway (по номеру), иначе текущий flashcall/SMS. */
    public LoginCodeResult sendLoginCode(String telephone) {
        if (telegramGatewayService.isEnabled()) {
            String code = generateLoginCode();
            String requestId = telegramGatewayService.checkSendAbility(telephone);
            if (requestId != null && telegramGatewayService.sendCode(telephone, code, requestId)) {
                return new LoginCodeResult(code, "telegram");
            }
        }
        return new LoginCodeResult(sendFlashcallCode(telephone), "call");
    }

    private String generateLoginCode() {
        return String.valueOf(RANDOM.nextInt(9000) + 1000);
    }
```

- [ ] **Step 4: Запустить — убедиться, что проходит**

Run: `cd backend && ./gradlew test --tests "ru.car.service.message.MessageServiceLoginCodeTest"`
Expected: PASS (3 теста).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/ru/car/service/message/MessageService.java backend/src/test/java/ru/car/service/message/MessageServiceLoginCodeTest.java
git commit -m "feat(auth): MessageService.sendLoginCode — выбор канала Telegram→flashcall"
```

---

### Task 4: Подключить sendLoginCode в LoginAuthMobileService + channel в ответе

**Files:**
- Modify: `backend/src/main/java/ru/car/dto/login_auth_mobile/LoginAuthMobileRsParams.java`
- Modify: `backend/src/main/java/ru/car/service/LoginAuthMobileService.java:40-53`
- Test: `backend/src/test/java/ru/car/service/LoginAuthMobileServiceTest.java` (Create)

**Interfaces:**
- Consumes: `MessageService.sendLoginCode(...)`, `MessageService.LoginCodeResult`.
- Produces: `LoginAuthMobileRsParams.channel` (String).

- [ ] **Step 1: Добавить поле `channel` в `LoginAuthMobileRsParams`**

```java
    @JsonProperty("channel")
    private String channel;
```
(добавить внутри класса после `timeToNextRequestSec`)

- [ ] **Step 2: Написать падающий тест `LoginAuthMobileServiceTest`**

```java
package ru.car.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.car.dto.login_auth_mobile.LoginAuthMobileRqParams;
import ru.car.dto.login_auth_mobile.LoginAuthMobileRsParams;
import ru.car.service.message.MessageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAuthMobileService Tests")
class LoginAuthMobileServiceTest {

    @Mock private MessageService messageService;
    @Mock private AuthenticationCodeService authenticationCodeService;

    @InjectMocks private LoginAuthMobileService service;

    @Test
    @DisplayName("loginMobile: шлёт код через sendLoginCode, сохраняет его и отдаёт channel")
    void loginMobileSendsAndStoresCode() {
        ReflectionTestUtils.setField(service, "adminPhone", "70000000000");
        LoginAuthMobileRqParams params = new LoginAuthMobileRqParams();
        params.setPhoneNumber("79991234567");
        when(authenticationCodeService.isAlreadySent("79991234567")).thenReturn(false);
        when(messageService.sendLoginCode("79991234567"))
                .thenReturn(new MessageService.LoginCodeResult("1234", "telegram"));

        LoginAuthMobileRsParams rs = service.loginMobile(params);

        verify(authenticationCodeService).create("79991234567", "1234");
        assertThat(rs.getChannel()).isEqualTo("telegram");
    }
}
```
> Примечание: проверь точное имя сеттера/поля телефона в `LoginAuthMobileRqParams` (Lombok `@Data` → `setPhoneNumber`). `adminCode` не нужен в этом тесте (ветка не-админа).

- [ ] **Step 3: Запустить — убедиться, что падает**

Run: `cd backend && ./gradlew test --tests "ru.car.service.LoginAuthMobileServiceTest"`
Expected: FAIL (`sendLoginCode`/`getChannel` ещё не используются в сервисе → `create` не вызывается с теми аргументами / channel null).

- [ ] **Step 4: Изменить `LoginAuthMobileService.loginMobile`** (строки 46-52)

Заменить:
```java
            String sendCode = messageService.sendFlashcallCode(telephone);
//            String sendCode = messageService.sendCallCode(telephone);
            authenticationCodeService.create(telephone, sendCode);
        }
        return LoginAuthMobileRsParams.builder()
                .timeToNextRequestSec(ApplicationConstants.SMS_NEXT_REQUEST_TIMEOUT_IN_SEC)
                .build();
```
на:
```java
            MessageService.LoginCodeResult result = messageService.sendLoginCode(telephone);
            authenticationCodeService.create(telephone, result.code());
            return LoginAuthMobileRsParams.builder()
                    .timeToNextRequestSec(ApplicationConstants.SMS_NEXT_REQUEST_TIMEOUT_IN_SEC)
                    .channel(result.channel())
                    .build();
        }
        return LoginAuthMobileRsParams.builder()
                .timeToNextRequestSec(ApplicationConstants.SMS_NEXT_REQUEST_TIMEOUT_IN_SEC)
                .build();
```
> Семантика: для админа (`adminPhone.equals(telephone)`) код не шлётся — возвращаем ответ без `channel`, как и раньше.

- [ ] **Step 5: Запустить — убедиться, что проходит**

Run: `cd backend && ./gradlew test --tests "ru.car.service.LoginAuthMobileServiceTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/ru/car/service/LoginAuthMobileService.java backend/src/main/java/ru/car/dto/login_auth_mobile/LoginAuthMobileRsParams.java backend/src/test/java/ru/car/service/LoginAuthMobileServiceTest.java
git commit -m "feat(auth): вход через Telegram Gateway с фолбэком; channel в ответе login_oauth_mobile"
```

---

### Task 5: Конфигурация + полный прогон + ops-заметки

**Files:**
- Modify: `backend/src/main/resources/application.yml` (блок `telegram:`)
- Modify: `backend/src/main/resources/application-prod.yml` (блок `telegram:`)
- Modify: `ai-ru/TECH_DEBT.md` (заметка про prod-включение)

**Interfaces:** нет нового кода.

- [ ] **Step 1: Добавить `gateway` в base `application.yml` (внутри `telegram:`, после блока `proxy:`)**

```yaml
  gateway:
    enabled: false
    baseUrl: https://gatewayapi.telegram.org
    accessToken: ""
    ttl: 300
    codeLength: 4
```

- [ ] **Step 2: Добавить `gateway` в `application-prod.yml` (внутри `telegram:`, после `proxy:`), пока ВЫКЛ**

```yaml
  gateway:
    # Включить ПОСЛЕ получения access token на gateway.telegram.org и пополнения баланса.
    # accessToken зашифровать Jasypt: ENC(...). Запросы идут через telegram.proxy (sing-box).
    enabled: false
    accessToken: ""
    ttl: 300
    codeLength: 4
```

- [ ] **Step 3: Полный прогон тестов и сборка jar**

Run: `cd backend && ./gradlew test bootJar`
Expected: BUILD SUCCESSFUL, все тесты зелёные.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/application.yml backend/src/main/resources/application-prod.yml
git commit -m "chore(config): telegram.gateway.* (по умолчанию выключен)"
```

- [ ] **Step 5: Ops-заметка** — записать в TECH_DEBT/память, что для включения на проде нужно: (1) аккаунт gateway.telegram.org + access token + баланс; (2) `JASYPT` ENC токена в `/var/www/car_id/application-prod.yml`; (3) `telegram.gateway.enabled: true`; (4) рестарт; (5) проверка: вход на номер с Telegram → код в чате «Verification Codes»; на номер без Telegram → flashcall/SMS.

---

## Self-Review

**Spec coverage:**
- Gateway-клиент через прокси → Task 1. checkSendAbility → Task 1. sendVerificationMessage → Task 2. Выбор канала + фолбэк → Task 3. Генерация+хранение кода → Task 3 (`generateLoginCode`) + Task 4 (`create`). Проверка кода без изменений → не трогаем (`confirmCode`). channel в ответе → Task 4. Конфиг + секрет ENC → Task 5. Ops-предусловия → Task 5 Step 5. Все пункты дизайна BF2 покрыты.

**Placeholder scan:** код приведён полностью в каждом шаге; «TODO» нет (ops-заметка Task5/Step5 — намеренно текст, не код).

**Type consistency:** `LoginCodeResult(code, channel)` — `.code()`/`.channel()` (record-аксессоры) используются единообразно в Task 3/4. `checkSendAbility: String` (null = недоступно), `sendCode: boolean`, `isEnabled: boolean` — согласованы между Task 1/2/3. `TelegramGatewayProperties` конструктор `(enabled, baseUrl, accessToken, ttl, codeLength)` — порядок совпадает с использованием в тесте Task 1 Step 3.

**Open verification points for implementer:** точное имя сеттера телефона в `LoginAuthMobileRqParams`; что `MockRestServiceServer` `content().formData(...)` доступен в версии spring-test проекта (если нет — заменить на проверку отдельных полей через `content().string(containsString(...))`).
