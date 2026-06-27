# VK ID Login (Backend) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Бэкенд-эндпоинт `POST /api/user.login_vk`: принимает VK `access_token`, проверяет его в VK `user_info`, по верифицированному номеру телефона выдаёт JWT.

**Architecture:** Идентичность по телефону — VK ID отдаёт проверенный номер, маппим на существующих пользователей через `userService.findOrCreateByPhoneNumberAndActivate`. Телефону с клиента не доверяем: берём только из server-to-server ответа VK. VK в РФ не блокируется → прокси не нужен. Переиспользуем существующую выдачу JWT.

**Tech Stack:** Java 17, Spring Boot 3.2, RestTemplate, Lombok, JUnit5 + Mockito + MockRestServiceServer.

## Global Constraints

- Идентичность = телефон; новых столбцов в `users` нет.
- Выдача JWT не меняется: `jwtService.generateToken(securityUserService.getDetails(user))`.
- По умолчанию `vk.enabled=false` → эндпоинт отвечает ошибкой, пока не настроен (включается ops-шагом после регистрации в VK).
- Секрет/идентификаторы (`vk.clientId`) — в конфиге; в prod не плейн-плейн (как остальные секреты на сервере).
- Конверт запроса/ответа как у существующих login-методов: `MobileRq<T>` → `MobileRs`.
- Коммиты частые. Тесты: `cd backend && ./gradlew test`.

**Verification point (VK docs):** точная структура ответа `user_info` и формат поля `phone` не подтверждены публично. DTO смоделирован как `{"user":{"user_id":..,"phone":..}}`; Jackson игнорирует неизвестные поля. При реализации сверить имена полей по доке VK ID; нормализация телефона устойчива к `+`, пробелам, скобкам.

---

### Task 1: VkIdService.fetchVerifiedPhone (+ properties, DTO, нормализация)

**Files:**
- Create: `backend/src/main/java/ru/car/service/vk/VkIdProperties.java`
- Create: `backend/src/main/java/ru/car/service/vk/dto/VkUserInfoResponse.java`
- Create: `backend/src/main/java/ru/car/service/vk/VkIdService.java`
- Modify: `backend/src/main/java/ru/car/enums/ErrorCode.java`
- Test: `backend/src/test/java/ru/car/service/vk/VkIdServiceTest.java`

**Interfaces:**
- Produces:
  - `VkIdService.isEnabled(): boolean`
  - `VkIdService.fetchVerifiedPhone(String accessToken): String` (нормализованный `7XXXXXXXXXX`; бросает `BadRequestException(ErrorCode.VK_AUTH_FAILED)` при ошибке/отсутствии телефона)
  - `VkIdProperties` (поля `enabled`, `clientId`, `userInfoUrl`)

- [ ] **Step 1: Добавить `VK_AUTH_FAILED` в `ErrorCode`** (после `SMS_ALREADY_SENT`)

```java
    SMS_ALREADY_SENT("SMS_ALREADY_SENT", ""),
    VK_AUTH_FAILED("VK_AUTH_FAILED", "Не удалось авторизоваться через VK");
```

- [ ] **Step 2: Создать `VkIdProperties`**

```java
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
```

- [ ] **Step 3: Создать DTO `VkUserInfoResponse`**

```java
package ru.car.service.vk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VkUserInfoResponse {
    private VkUser user;

    @Data
    @NoArgsConstructor
    public static class VkUser {
        @JsonProperty("user_id")
        private String userId;
        private String phone;
    }
}
```

- [ ] **Step 4: Написать падающий тест `VkIdServiceTest`**

```java
package ru.car.service.vk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.car.exception.BadRequestException;
import ru.car.test.base.BaseUnitTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("VkIdService Tests")
class VkIdServiceTest extends BaseUnitTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private VkIdService service;

    private static final String URL = "https://id.vk.ru/oauth2/user_info";

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        service = new VkIdService(new VkIdProperties(true, "12345", URL), restTemplate);
    }

    @Test
    @DisplayName("возвращает нормализованный телефон при ok")
    void returnsNormalizedPhone() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"user\":{\"user_id\":\"42\",\"phone\":\"+7 (999) 123-45-67\"}}",
                        MediaType.APPLICATION_JSON));

        assertThat(service.fetchVerifiedPhone("tok")).isEqualTo("79991234567");
        server.verify();
    }

    @Test
    @DisplayName("бросает VK_AUTH_FAILED, если телефона нет в ответе")
    void throwsWhenNoPhone() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"user\":{\"user_id\":\"42\"}}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.fetchVerifiedPhone("tok"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("бросает VK_AUTH_FAILED при HTTP-ошибке (невалидный токен)")
    void throwsOnHttpError() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> service.fetchVerifiedPhone("tok"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("нормализация: 8XXXXXXXXXX → 7XXXXXXXXXX")
    void normalizesLeadingEight() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"user\":{\"phone\":\"89991234567\"}}", MediaType.APPLICATION_JSON));

        assertThat(service.fetchVerifiedPhone("tok")).isEqualTo("79991234567");
    }
}
```

- [ ] **Step 5: Запустить — убедиться, что не компилируется/падает**

Run: `cd backend && ./gradlew test --tests "ru.car.service.vk.VkIdServiceTest"`
Expected: FAIL (нет `VkIdService`).

- [ ] **Step 6: Реализовать `VkIdService`**

```java
package ru.car.service.vk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.car.enums.ErrorCode;
import ru.car.exception.BadRequestException;
import ru.car.service.vk.dto.VkUserInfoResponse;

@Slf4j
@Component
@EnableConfigurationProperties(VkIdProperties.class)
public class VkIdService {

    private final VkIdProperties properties;
    private final RestTemplate restTemplate;

    @Autowired
    public VkIdService(VkIdProperties properties) {
        this(properties, buildRestTemplate());
    }

    // package-private — для тестов с MockRestServiceServer
    VkIdService(VkIdProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(properties.getEnabled());
    }

    /** Проверяет VK access_token в user_info и возвращает нормализованный телефон. */
    public String fetchVerifiedPhone(String accessToken) {
        VkUserInfoResponse resp;
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", properties.getClientId());
            body.add("access_token", accessToken);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            resp = restTemplate.exchange(properties.getUserInfoUrl(), HttpMethod.POST,
                    new HttpEntity<>(body, headers), VkUserInfoResponse.class).getBody();
        } catch (Exception e) {
            log.warn("VK ID user_info ошибка: {}", e.getMessage());
            throw new BadRequestException("VK auth failed", ErrorCode.VK_AUTH_FAILED);
        }
        String phone = resp != null && resp.getUser() != null ? resp.getUser().getPhone() : null;
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("VK не вернул телефон (нет scope phone?)", ErrorCode.VK_AUTH_FAILED);
        }
        return normalizePhone(phone);
    }

    /** Приводит к виду 7XXXXXXXXXX (как users.phone_number). */
    static String normalizePhone(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 11 && digits.startsWith("8")) {
            digits = "7" + digits.substring(1);
        }
        if (digits.length() == 10) {
            digits = "7" + digits;
        }
        return digits;
    }

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }
}
```

- [ ] **Step 7: Запустить — убедиться, что проходит**

Run: `cd backend && ./gradlew test --tests "ru.car.service.vk.VkIdServiceTest"`
Expected: PASS (4 теста).

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/ru/car/service/vk/ backend/src/main/java/ru/car/enums/ErrorCode.java backend/src/test/java/ru/car/service/vk/
git commit -m "feat(vk): VkIdService.fetchVerifiedPhone — проверка VK access_token через user_info"
```

---

### Task 2: Эндпоинт user.login_vk (сервис + DTO + контроллер + whitelist + конфиг)

**Files:**
- Create: `backend/src/main/java/ru/car/dto/login_vk/LoginVkRqParams.java`
- Modify: `backend/src/main/java/ru/car/service/LoginAuthMobileService.java`
- Modify: `backend/src/main/java/ru/car/controller/LoginAuthMobileController.java`
- Modify: `backend/src/main/java/ru/car/config/security/SecurityConfiguration.java` (AUTH_WHITELIST)
- Modify: `backend/src/main/resources/application.yml`, `backend/src/main/resources/application-prod.yml`
- Test: `backend/src/test/java/ru/car/service/LoginAuthMobileVkTest.java`

**Interfaces:**
- Consumes: `VkIdService.isEnabled()/fetchVerifiedPhone(...)`, `userService.findOrCreateByPhoneNumberAndActivate(String, Role)`, `jwtService.generateToken(...)`, `securityUserService.getDetails(user)`.
- Produces: `LoginAuthMobileService.loginVk(LoginVkRqParams): LoginAuthCodeRsParams`; HTTP `POST /api/user.login_vk`.

- [ ] **Step 1: Создать `LoginVkRqParams`**

```java
package ru.car.dto.login_vk;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVkRqParams {
    @JsonProperty("access_token")
    private String accessToken;
}
```

- [ ] **Step 2: Написать падающий тест `LoginAuthMobileVkTest`**

```java
package ru.car.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.car.dto.login_oauth_code.LoginAuthCodeRsParams;
import ru.car.dto.login_vk.LoginVkRqParams;
import ru.car.enums.Role;
import ru.car.exception.BadRequestException;
import ru.car.model.User;
import ru.car.model.security.SecurityUser;
import ru.car.service.security.JwtService;
import ru.car.service.security.SecurityUserService;
import ru.car.service.vk.VkIdService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAuthMobileService.loginVk Tests")
class LoginAuthMobileVkTest {

    @Mock private VkIdService vkIdService;
    @Mock private UserService userService;
    @Mock private JwtService jwtService;
    @Mock private SecurityUserService securityUserService;

    @InjectMocks private LoginAuthMobileService service;

    @Test
    @DisplayName("loginVk: телефон из VK → JWT")
    void loginVkReturnsToken() {
        User user = User.builder().id(1L).phoneNumber("79991234567").build();
        when(vkIdService.isEnabled()).thenReturn(true);
        when(vkIdService.fetchVerifiedPhone("tok")).thenReturn("79991234567");
        when(userService.findOrCreateByPhoneNumberAndActivate("79991234567", Role.ROLE_USER)).thenReturn(user);
        SecurityUser details = SecurityUser.builder().id(1L).telephone("79991234567").build();
        when(securityUserService.getDetails(user)).thenReturn(details);
        when(jwtService.generateToken(details)).thenReturn("JWT");

        LoginAuthCodeRsParams rs = service.loginVk(LoginVkRqParams.builder().accessToken("tok").build());

        assertThat(rs.getToken()).isEqualTo("JWT");
    }

    @Test
    @DisplayName("loginVk: при выключенном VK — ошибка")
    void loginVkDisabled() {
        when(vkIdService.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> service.loginVk(LoginVkRqParams.builder().accessToken("tok").build()))
                .isInstanceOf(BadRequestException.class);
    }
}
```
> Примечание: сверить билдеры `User`/`SecurityUser` (поля). Если у `SecurityUser` нет `@Builder` — сконструировать через доступный конструктор/моки.

- [ ] **Step 3: Запустить — убедиться, что падает**

Run: `cd backend && ./gradlew test --tests "ru.car.service.LoginAuthMobileVkTest"`
Expected: FAIL (нет `loginVk` / поля `vkIdService`).

- [ ] **Step 4: Реализовать `loginVk` в `LoginAuthMobileService`**

4a. Импорты:
```java
import ru.car.dto.login_vk.LoginVkRqParams;
import ru.car.service.vk.VkIdService;
```
4b. Поле (в блок зависимостей):
```java
    private final VkIdService vkIdService;
```
4c. Метод (после `confirmCode`):
```java
    @Transactional
    public LoginAuthCodeRsParams loginVk(LoginVkRqParams params) {
        if (!vkIdService.isEnabled()) {
            throw new BadRequestException("VK login disabled", ErrorCode.VK_AUTH_FAILED);
        }
        String phone = vkIdService.fetchVerifiedPhone(params.getAccessToken());
        User user = userService.findOrCreateByPhoneNumberAndActivate(phone, Role.ROLE_USER);
        return LoginAuthCodeRsParams.builder()
                .token(jwtService.generateToken(securityUserService.getDetails(user)))
                .build();
    }
```
> `ErrorCode`, `Role`, `User`, `LoginAuthCodeRsParams` уже импортированы в этом файле (используются в `confirmCode`); добавить недостающие при необходимости.

- [ ] **Step 5: Запустить — убедиться, что проходит**

Run: `cd backend && ./gradlew test --tests "ru.car.service.LoginAuthMobileVkTest"`
Expected: PASS (2 теста).

- [ ] **Step 6: Добавить эндпоинт в `LoginAuthMobileController`** (после `confirmCode`)

```java
    @Operation(summary = "Login via VK ID")
    @PostMapping("user.login_vk")
    public ResponseEntity<MobileRs<?>> loginVk(@RequestBody MobileRq<ru.car.dto.login_vk.LoginVkRqParams> request) {
        log.debug("принят запрос на вход через VK");
        return ResponseEntity.ok(MobileRs.builder()
                .result("true")
                .method(request.getMethod())
                .params(loginAuthMobileService.loginVk(request.getParams()))
                .build());
    }
```

- [ ] **Step 7: Добавить путь в whitelist `SecurityConfiguration`**

Рядом с `"/api/user.login_oauth_code"` добавить:
```java
            "/api/user.login_vk",
```

- [ ] **Step 8: Конфиг — `vk` блок (выключен) в base и prod**

`application.yml`:
```yaml
vk:
  enabled: false
  clientId: ""
  userInfoUrl: https://id.vk.ru/oauth2/user_info
```
`application-prod.yml` (включить после регистрации приложения в VK):
```yaml
vk:
  enabled: false
  clientId: ""
  userInfoUrl: https://id.vk.ru/oauth2/user_info
```

- [ ] **Step 9: Полный прогон + сборка**

Run: `cd backend && ./gradlew test bootJar`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 10: Commit**

```bash
git add backend/
git commit -m "feat(vk): эндпоинт user.login_vk (вход по верифицированному телефону VK → JWT)"
```

---

## Self-Review

**Spec coverage:** проверка VK-токена → Task 1 (`fetchVerifiedPhone`); идентичность по телефону + JWT → Task 2 (`loginVk` → `findOrCreateByPhoneNumberAndActivate` → `generateToken`); эндпоинт + whitelist → Task 2; конфиг/флаг → Task 2; «не доверяем клиенту» → телефон только из `user_info`. Мобайл — вне объёма (по решению). Покрыто.

**Placeholder scan:** код приведён полностью; «verification point» по полям `user_info` — намеренный (внешняя зависимость), с устойчивой нормализацией и Jackson ignore-unknown.

**Type consistency:** `fetchVerifiedPhone: String`, `isEnabled: boolean` — согласованы Task1/2. `VkIdProperties(enabled, clientId, userInfoUrl)` — порядок совпадает с тестом Task1/Step4. `loginVk(LoginVkRqParams): LoginAuthCodeRsParams` — совпадает в сервисе/контроллере/тесте.

**Open verification points:** билдеры `User`/`SecurityUser` в тесте Task2 (сверить наличие `@Builder`/полей); точные поля ответа `user_info` VK (сверить по доке, поправить DTO при расхождении).
