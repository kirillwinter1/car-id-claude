# BF5 «Позвонить владельцу» — Фаза 1 (backend + web)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Прохожий видит на экране статуса `tel:`-ссылку «Позвонить владельцу: +7…», если владелец это разрешил и прошло ~60с после отправки.

**Architecture:** Переиспользуем `GET /api/notification/{id}/status` (Тир 2): добавляем nullable `owner_phone`, который сервер заполняет только при гейтинге (настройка владельца ON + прошёл порог задержки, независимо от read/unread). Настройка `show_phone_on_unreachable` хранится в `notification_settings` (дефолт FALSE), управляется через существующий patch настроек (opt-in). Фронт рендерит блок звонка.

**Tech Stack:** Java 17, Spring Boot 3.2, JdbcTemplate, Liquibase, MapStruct, JUnit5+Mockito; frontend vanilla JS.

## Global Constraints

- **Дефолт настройки — FALSE для всех** (включая новых). Дефолт-ON для новых + согласие при регистрации — отдельная Фаза 2, в этот план НЕ входит.
- Номер показывается **независимо от READ** (решение брейншторма): гейтинг = настройка ON + прошёл порог.
- Порог раскрытия — **60 секунд** от `notification.created_date`.
- Номер отдаётся только по `notificationId` (через status-эндпоинт), НЕ из `GET /api/qr/{uuid}`.
- Формат номера в ответе — `+7XXXXXXXXXX` (`"+" + users.phone_number`).
- Миграция Liquibase: новый `changelog-1.18.xml` + строка в `master.xml`.
- Коммиты частые. Тесты: `cd backend && ./gradlew test`.

---

### Task 1: Колонка show_phone_on_unreachable (миграция + модель + репозиторий)

**Files:**
- Create: `backend/src/main/resources/db/changelog/changelog-1.18.xml`
- Modify: `backend/src/main/resources/db/master.xml`
- Modify: `backend/src/main/java/ru/car/model/NotificationSetting.java`
- Modify: `backend/src/main/java/ru/car/repository/NotificationSettingRepository.java:25` (UPDATE SQL)
- Test: `backend/src/test/java/ru/car/repository/NotificationSettingRepositoryIntegrationTest.java`

**Interfaces:**
- Produces: `NotificationSetting.getShowPhoneOnUnreachable(): Boolean` / `setShowPhoneOnUnreachable(Boolean)`; колонка `notification_settings.show_phone_on_unreachable` (default false); `update(...)` персистит это поле.

- [ ] **Step 1: Создать миграцию `changelog-1.18.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
		    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.4.xsd">

    <changeSet id="1.18.1" author="claude">
        <addColumn tableName="notification_settings" schemaName="${schema}">
            <column name="show_phone_on_unreachable" type="boolean" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
        </addColumn>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 2: Подключить миграцию в `master.xml`** (после строки include для `changelog-1.17.xml`)

```xml
    <include file="changelog/changelog-1.18.xml" relativeToChangelogFile="true"/>
```

- [ ] **Step 3: Добавить поле в модель `NotificationSetting`** (после `telegramDialogId`)

```java
    private Boolean showPhoneOnUnreachable;
```

- [ ] **Step 4: Добавить поле в UPDATE SQL репозитория** (`NotificationSettingRepository.java:25`)

Заменить строку `UPDATE_ALL_BY_USER_ID`:
```java
    private static final String UPDATE_ALL_BY_USER_ID = "UPDATE notification_settings SET push_enabled = :pushEnabled, call_enabled = :callEnabled, telegram_enabled = :telegramEnabled, active = :active, telegram_dialog_id = :telegramDialogId, show_phone_on_unreachable = :showPhoneOnUnreachable WHERE user_id = :userId";
```
> `SELECT n.*` + `BeanPropertyRowMapper` уже подхватит новую колонку в поле `showPhoneOnUnreachable` автоматически. `INSERT_NEW` не трогаем — новым строкам колонка проставится дефолтом БД (false).

- [ ] **Step 5: Добавить падающий тест в `NotificationSettingRepositoryIntegrationTest`** (внутри класса)

```java
    @Nested
    @DisplayName("showPhoneOnUnreachable")
    class ShowPhoneOnUnreachable {

        @Test
        @DisplayName("по умолчанию false и обновляется через update")
        void defaultsFalseAndUpdatable() {
            notificationSettingRepository.save(ru.car.model.NotificationSetting.builder()
                    .userId(userWithoutSettings)
                    .pushEnabled(true).callEnabled(false).telegramEnabled(false).active(true)
                    .build());

            ru.car.model.NotificationSetting saved = notificationSettingRepository.findByUserId(userWithoutSettings);
            assertThat(saved.getShowPhoneOnUnreachable()).isFalse();

            saved.setShowPhoneOnUnreachable(true);
            notificationSettingRepository.update(saved);

            assertThat(notificationSettingRepository.findByUserId(userWithoutSettings).getShowPhoneOnUnreachable()).isTrue();
        }
    }
```
> `userWithoutSettings` уже создаётся в `@BeforeEach` этого теста, но там для него НЕ создаются настройки — здесь создаём свои. Если конфликт — используй новый тестовый телефон через `createTestUser(...)`.

- [ ] **Step 6: Прогон**

Run: `cd backend && ./gradlew test --tests "ru.car.repository.NotificationSettingRepositoryIntegrationTest"`
Expected: сперва FAIL (нет геттера/колонки), после Step 1-4 — PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/resources/db/ backend/src/main/java/ru/car/model/NotificationSetting.java backend/src/main/java/ru/car/repository/NotificationSettingRepository.java backend/src/test/java/ru/car/repository/NotificationSettingRepositoryIntegrationTest.java
git commit -m "feat(bf5): колонка show_phone_on_unreachable в notification_settings"
```

---

### Task 2: owner_phone в статусе уведомления (гейтинг)

**Files:**
- Modify: `backend/src/main/java/ru/car/dto/NotificationStatusDto.java`
- Modify: `backend/src/main/java/ru/car/service/NotificationService.java` (поле `userRepository`, `getStatus`, helper)
- Test: `backend/src/test/java/ru/car/service/NotificationServiceTest.java`

**Interfaces:**
- Consumes: `NotificationSetting.getShowPhoneOnUnreachable()` (Task 1); `UserRepository.findById(Long): Optional<User>`; `Notification.getCreatedDate()`.
- Produces: `NotificationStatusDto.ownerPhone` (String, `+7…` или null) в ответе `/api/notification/{id}/status`.

- [ ] **Step 1: Добавить поле в `NotificationStatusDto`** (после `callEnabled`)

```java
    /** Реальный номер владельца для tel:-ссылки (BF5). Не null только если владелец разрешил
     *  показ и прошёл порог задержки. Иначе null. */
    @JsonProperty("owner_phone")
    private String ownerPhone;
```

- [ ] **Step 2: Написать падающие тесты в `NotificationServiceTest`** (новый @Nested-класс)

```java
    @Nested
    @DisplayName("getStatus ownerPhone (BF5)")
    class GetStatusOwnerPhone {

        @Test
        @DisplayName("отдаёт +телефон, если настройка ON и прошёл порог")
        void returnsPhoneWhenAllowedAndDelayPassed() {
            java.util.UUID nid = java.util.UUID.randomUUID();
            java.util.UUID qrId = java.util.UUID.randomUUID();
            Notification n = NotificationBuilder.aNotification()
                    .withId(nid).withQrId(qrId).withStatus(NotificationStatus.READ)
                    .withCreatedDate(java.time.LocalDateTime.now().minusSeconds(120)).build();
            when(notificationRepository.findById(nid)).thenReturn(Optional.of(n));
            NotificationSetting setting = NotificationSetting.builder()
                    .userId(7L).showPhoneOnUnreachable(true).callEnabled(false).build();
            when(notificationSettingRepository.findByQrId(qrId)).thenReturn(setting);
            when(userRepository.findById(7L)).thenReturn(Optional.of(
                    ru.car.model.User.builder().id(7L).phoneNumber("79991234567").build()));

            ru.car.dto.NotificationStatusDto dto = notificationService.getStatus(nid);

            assertThat(dto.getOwnerPhone()).isEqualTo("+79991234567");
        }

        @Test
        @DisplayName("null, если настройка OFF")
        void nullWhenSettingOff() {
            java.util.UUID nid = java.util.UUID.randomUUID();
            java.util.UUID qrId = java.util.UUID.randomUUID();
            Notification n = NotificationBuilder.aNotification()
                    .withId(nid).withQrId(qrId).withStatus(NotificationStatus.UNREAD)
                    .withCreatedDate(java.time.LocalDateTime.now().minusSeconds(120)).build();
            when(notificationRepository.findById(nid)).thenReturn(Optional.of(n));
            when(notificationSettingRepository.findByQrId(qrId)).thenReturn(
                    NotificationSetting.builder().userId(7L).showPhoneOnUnreachable(false).callEnabled(false).build());

            assertThat(notificationService.getStatus(nid).getOwnerPhone()).isNull();
        }

        @Test
        @DisplayName("null, если порог ещё не прошёл")
        void nullWhenTooEarly() {
            java.util.UUID nid = java.util.UUID.randomUUID();
            java.util.UUID qrId = java.util.UUID.randomUUID();
            Notification n = NotificationBuilder.aNotification()
                    .withId(nid).withQrId(qrId).withStatus(NotificationStatus.UNREAD)
                    .withCreatedDate(java.time.LocalDateTime.now().minusSeconds(5)).build();
            when(notificationRepository.findById(nid)).thenReturn(Optional.of(n));
            when(notificationSettingRepository.findByQrId(qrId)).thenReturn(
                    NotificationSetting.builder().userId(7L).showPhoneOnUnreachable(true).callEnabled(false).build());

            assertThat(notificationService.getStatus(nid).getOwnerPhone()).isNull();
        }
    }
```
> Сверь API билдера `NotificationBuilder` (методы `withId/withQrId/withStatus/withCreatedDate`) — он используется в этом же тест-классе для других тестов; подставь реальные имена методов. Добавь `@Mock private UserRepository userRepository;` в поля теста (Mockito прокинет его в `@InjectMocks`). `findByIdOrThrowNotFound` внутри использует `notificationRepository.findById` — мокаем его.

- [ ] **Step 3: Запустить — убедиться, что падает**

Run: `cd backend && ./gradlew test --tests "ru.car.service.NotificationServiceTest"`
Expected: FAIL (нет `ownerPhone`/`userRepository`).

- [ ] **Step 4: Реализовать в `NotificationService`**

4a. Добавить импорт и поле зависимости (рядом с другими `private final ... Repository`):
```java
import ru.car.repository.UserRepository;
```
```java
    private final UserRepository userRepository;
```
4b. Константа порога (в начало класса, рядом с полями):
```java
    private static final long REVEAL_DELAY_SEC = 60;
```
4c. Заменить тело `getStatus` и добавить helper:
```java
    @Transactional
    public NotificationStatusDto getStatus(UUID id) {
        Notification notification = findByIdOrThrowNotFound(id);
        NotificationSetting setting = notificationSettingRepository.findByQrId(notification.getQrId());
        boolean callEnabled = setting != null && Boolean.TRUE.equals(setting.getCallEnabled());
        return NotificationStatusDto.builder()
                .notificationId(id)
                .status(notification.getStatus())
                .callEnabled(callEnabled)
                .ownerPhone(resolveOwnerPhone(notification, setting))
                .build();
    }

    /** BF5: номер владельца прохожему — только если владелец разрешил и прошёл порог задержки.
     *  Показывается независимо от read/unread. */
    private String resolveOwnerPhone(Notification notification, NotificationSetting setting) {
        if (setting == null || !Boolean.TRUE.equals(setting.getShowPhoneOnUnreachable())) {
            return null;
        }
        if (notification.getCreatedDate() == null
                || notification.getCreatedDate().isAfter(LocalDateTime.now().minusSeconds(REVEAL_DELAY_SEC))) {
            return null;
        }
        return userRepository.findById(setting.getUserId())
                .map(u -> "+" + u.getPhoneNumber())
                .orElse(null);
    }
```
> `LocalDateTime` уже импортирован в файле. `User`/`Optional` — проверь импорты, добавь при необходимости.

- [ ] **Step 5: Запустить — PASS**

Run: `cd backend && ./gradlew test --tests "ru.car.service.NotificationServiceTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/ru/car/dto/NotificationStatusDto.java backend/src/main/java/ru/car/service/NotificationService.java backend/src/test/java/ru/car/service/NotificationServiceTest.java
git commit -m "feat(bf5): owner_phone в статусе уведомления с гейтингом (настройка + порог 60с)"
```

---

### Task 3: Управление настройкой владельца (DTO + mapper) — opt-in через patch

**Files:**
- Modify: `backend/src/main/java/ru/car/dto/NotificationSettingDto.java`
- Modify: `backend/src/main/java/ru/car/mapper/NotificationSettingDtoMapper.java`
- Test: `backend/src/test/java/ru/car/service/NotificationSettingServiceTest.java`

**Interfaces:**
- Produces: поле `NotificationSettingDto.showPhoneOnUnreachable`; `patch(...)` сохраняет его (через `updateIgnoreNull` + `update` из Task 1).

- [ ] **Step 1: Добавить поле в `NotificationSettingDto`** (после `telegramDialogId`)

```java
    @JsonProperty("show_phone_on_unreachable")
    private Boolean showPhoneOnUnreachable;
```

- [ ] **Step 2: Добавить маппинг в `NotificationSettingDtoMapper.updateIgnoreNull`** (ещё одна `@Mapping`-строка)

```java
    @Mapping(target = "showPhoneOnUnreachable", source = "showPhoneOnUnreachable", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
```
> `toDto`/`toEntity` (без `@Mapping`) MapStruct смапит по имени автоматически.

- [ ] **Step 3: Падающий тест в `NotificationSettingServiceTest`**

```java
    @Test
    @DisplayName("patch включает show_phone_on_unreachable")
    void patchEnablesShowPhone() {
        Long userId = 7L;
        NotificationSetting existing = NotificationSetting.builder()
                .userId(userId).pushEnabled(true).callEnabled(false)
                .telegramEnabled(false).active(true).showPhoneOnUnreachable(false).build();
        when(notificationSettingRepository.findByUserId(userId)).thenReturn(existing);
        when(notificationSettingRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationSettingDto patch = NotificationSettingDto.builder().showPhoneOnUnreachable(true).build();
        notificationSettingService.patch(userId, patch);

        org.mockito.ArgumentCaptor<NotificationSetting> captor = org.mockito.ArgumentCaptor.forClass(NotificationSetting.class);
        verify(notificationSettingRepository).update(captor.capture());
        assertThat(captor.getValue().getShowPhoneOnUnreachable()).isTrue();
    }
```
> Сверь имена моков/полей в существующем `NotificationSettingServiceTest` (объект сервиса, мок репозитория, мок маппера). Если маппер замокан — `updateIgnoreNull` не сработает по-настоящему; тогда либо используй реальный маппер (`Mappers.getMapper(...)`/Spring), либо проверь вызов с нужным DTO. Подстройся под существующий стиль теста.

- [ ] **Step 4: Запустить (FAIL → PASS)**

Run: `cd backend && ./gradlew test --tests "ru.car.service.NotificationSettingServiceTest"`

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/ru/car/dto/NotificationSettingDto.java backend/src/main/java/ru/car/mapper/NotificationSettingDtoMapper.java backend/src/test/java/ru/car/service/NotificationSettingServiceTest.java
git commit -m "feat(bf5): show_phone_on_unreachable в настройках (opt-in через patch)"
```

---

### Task 4: Frontend — блок «Позвонить владельцу»

**Files:**
- Modify: `frontend/js/sendMsg.js`
- Modify: `frontend/js/systemMessages.js`

**Interfaces:**
- Consumes: поле `owner_phone` из `GET /api/notification/{id}/status` (Task 2).

- [ ] **Step 1: Добавить константу в `systemMessages.js`**

```javascript
const STATUS_CALL_OWNER = "Позвонить владельцу";
```

- [ ] **Step 2: Рендерить блок при наличии owner_phone** — в `sendMsg.js`, в `checkMsgStatus()` после строки `callEnabled = msgStatus.call_enabled === true;`

```javascript
            if (msgStatus.owner_phone) {
                showOwnerCallButton(msgStatus.owner_phone);
            }
```

- [ ] **Step 3: Добавить функцию рендера** (рядом с другими функциями в `sendMsg.js`)

```javascript
function showOwnerCallButton(phone) {
    if (document.getElementById("owner-call-link")) return; // уже показан
    const container = document.querySelector("#delivery-status") || document.body;
    const a = document.createElement("a");
    a.id = "owner-call-link";
    a.href = "tel:" + phone;
    a.className = "owner-call-button";
    a.textContent = STATUS_CALL_OWNER + ": " + phone;
    container.appendChild(a);
}
```
> Сверь реальный контейнер статуса в `notification.html` (`#delivery-status` использовался в Тир 2). Стиль `.owner-call-button` — добавить в `frontend/css/style.css` под существующие кнопки (по образцу).

- [ ] **Step 4: Ручная проверка (frontend без автотестов)**

Поднять локально (`cd frontend && python3 -m http.server 8000`) с бэкендом, у тестового владельца включить `show_phone_on_unreachable` (через patch), отправить уведомление, подождать >60с → на странице статуса появляется «Позвонить владельцу: +7…» (`tel:`). Проверить, что при OFF/раньше 60с блока нет.

- [ ] **Step 5: Commit**

```bash
git add frontend/js/sendMsg.js frontend/js/systemMessages.js frontend/css/style.css
git commit -m "feat(bf5): блок «Позвонить владельцу» (tel:) на экране статуса"
```

---

### Task 5: Полный прогон + сборка

- [ ] **Step 1:** `cd backend && ./gradlew test bootJar` → BUILD SUCCESSFUL.
- [ ] **Step 2:** Commit (если остались несведённые мелочи) — иначе пропустить.

---

## Self-Review

**Spec coverage:** колонка + дефолт FALSE → Task 1; `owner_phone` через status + гейтинг (настройка + порог 60с, независимо от READ) → Task 2; управление настройкой (opt-in) → Task 3; web-блок tel: → Task 4. Антискрейпинг (только по notificationId) — обеспечивается тем, что номер только в status-эндпоинте, не в `qr/{uuid}`. Дефолт-ON для новых + согласие — **намеренно вне Фазы 1** (отмечено в Global Constraints; Фаза 2). Мобайл/Telegram тумблер — Фаза 2.

**Placeholder scan:** код полный; «сверь билдер/моки» — указания свериться с существующим тест-кодом (имена методов билдера `NotificationBuilder`, стиль `NotificationSettingServiceTest`), не плейсхолдеры логики.

**Type consistency:** `showPhoneOnUnreachable` (Boolean) — единое имя в модели/DTO/SQL/mapper/тестах. `ownerPhone` (String, `+7…`) — DTO + getStatus + тесты. `REVEAL_DELAY_SEC=60`. `resolveOwnerPhone(Notification, NotificationSetting)`.

**Open verification points:** API билдера `NotificationBuilder` в тестах Task 2; устройство `NotificationSettingServiceTest` (реальный vs замоканный маппер) в Task 3; реальный контейнер статуса в `notification.html` для Task 4.

## Не входит (Фаза 2, отдельный план)

- Дефолт TRUE для новых пользователей (в `NotificationSettingService.create`) + **согласие при регистрации** (152-ФЗ, юр. текст).
- Тумблер в мобильном приложении и в Telegram-боте.
- (Опц.) метрика «номер раскрыт».
