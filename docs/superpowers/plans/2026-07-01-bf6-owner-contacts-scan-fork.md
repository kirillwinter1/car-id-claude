# BF6 «Контакты владельца + развилка на скане» Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** После скана метки прохожий видит развилку — «Сообщить о событии» / «Написать в Telegram·VK·MAX» / «Позвонить» — по контактам, которые владелец опубликовал.

**Architecture:** Владелец хранит контакты в `notification_settings` (telegram/vk/max) + флаг телефона. `GET /api/qr/{uuid}` отдаёт готовые ссылки в `owner_contacts`. Фронт `qr.html` рендерит кнопки. Владелец задаёт контакты в приложении (один релиз).

**Tech Stack:** Backend: Java 17, Spring Boot, JdbcTemplate, Liquibase, MapStruct, JUnit5+Mockito. Web: vanilla JS. Mobile: Flutter/GetX.

## Global Constraints

- Контакты видны сразу на скане; защита — неугадываемый UUID метки + opt-in владельца (пусто = кнопки нет).
- Мессенджеры opt-in по наличию значения. Телефон — по `show_phone_on_unreachable`; **дефолт TRUE для новых**, FALSE для существующих.
- ⚠️ Дефолт-ON телефона деплоить на прод **только после обновления Политики/Оферты** (152-ФЗ, владелец).
- Ссылки строит бэкенд при отдаче (хранит сырой ввод владельца): TG `https://t.me/<h>`, VK `https://vk.me/<h>`, MAX — ссылка `max.ru/...` как есть (+`https://` если нет схемы).
- Миграция Liquibase: `changelog-1.19.xml` + строка в `master.xml`.
- Сборка/публикация приложения (RuStore/App Store) — вне плана (владелец).
- Backend-тесты: `cd backend && ./gradlew test`. Mobile: `cd mobile && flutter test`.

---

## Фаза A — Backend

### Task A1: contact-поля в notification_settings + дефолт-ON телефона для новых

**Files:**
- Create: `backend/src/main/resources/db/changelog/changelog-1.19.xml`
- Modify: `backend/src/main/resources/db/master.xml`
- Modify: `backend/src/main/java/ru/car/model/NotificationSetting.java`
- Modify: `backend/src/main/java/ru/car/repository/NotificationSettingRepository.java` (INSERT_NEW + UPDATE_ALL_BY_USER_ID)
- Modify: `backend/src/main/java/ru/car/service/NotificationSettingService.java` (create)
- Test: `backend/src/test/java/ru/car/repository/NotificationSettingRepositoryIntegrationTest.java`
- Test: `backend/src/test/java/ru/car/service/NotificationSettingServiceTest.java`

**Interfaces:**
- Produces: `NotificationSetting.getTelegramContact()/getVkContact()/getMaxContact()`; колонки `telegram_contact/vk_contact/max_contact`; новые строки создаются с `show_phone_on_unreachable=true`; `update()` персистит contact-поля.

- [ ] **Step 1: Миграция `changelog-1.19.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
		    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.4.xsd">

    <changeSet id="1.19.1" author="claude">
        <addColumn tableName="notification_settings" schemaName="${schema}">
            <column name="telegram_contact" type="varchar(128)"/>
            <column name="vk_contact" type="varchar(256)"/>
            <column name="max_contact" type="varchar(256)"/>
        </addColumn>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 2: `master.xml`** — добавить после include `changelog-1.18.xml`:
```xml
    <include file="changelog/changelog-1.19.xml" relativeToChangelogFile="true"/>
```

- [ ] **Step 3: Модель `NotificationSetting`** — после `showPhoneOnUnreachable`:
```java
    private String telegramContact;
    private String vkContact;
    private String maxContact;
```

- [ ] **Step 4: Репозиторий SQL** — заменить `INSERT_NEW` и `UPDATE_ALL_BY_USER_ID`:
```java
    private static final String INSERT_NEW = "INSERT INTO notification_settings (user_id, push_enabled, call_enabled, telegram_enabled, active, show_phone_on_unreachable) VALUES (:userId, :pushEnabled, :callEnabled, :telegramEnabled, :active, :showPhoneOnUnreachable)";
    private static final String UPDATE_ALL_BY_USER_ID = "UPDATE notification_settings SET push_enabled = :pushEnabled, call_enabled = :callEnabled, telegram_enabled = :telegramEnabled, active = :active, telegram_dialog_id = :telegramDialogId, show_phone_on_unreachable = :showPhoneOnUnreachable, telegram_contact = :telegramContact, vk_contact = :vkContact, max_contact = :maxContact WHERE user_id = :userId";
```
> `SELECT n.*` + `BeanPropertyRowMapper` подхватят новые колонки. `INSERT_NEW` теперь пишет `show_phone_on_unreachable` (для дефолт-ON), contact-поля не пишет → NULL по умолчанию.

- [ ] **Step 5: `NotificationSettingService.create`** — задать дефолт-ON телефона:
```java
        notificationSettingRepository.save(NotificationSetting.builder()
                .userId(userId)
                .pushEnabled(true)
                .callEnabled(false)
                .telegramEnabled(false)
                .active(true)
                .showPhoneOnUnreachable(true)
                .build());
```
Также в `deleteByUserId` (builder для `update`) добавить `.showPhoneOnUnreachable(false)` — иначе `UPDATE ... = NULL` (у колонки нет NOT NULL, но для консистентности). Contact-поля в билдере не заданы → в UPDATE уйдут NULL (обнулят контакты при удалении аккаунта — корректно).

- [ ] **Step 6: Тест сервиса** (`NotificationSettingServiceTest`, новый @Nested):
```java
    @Nested
    @DisplayName("create")
    class Create {
        @Test
        @DisplayName("новый пользователь: showPhoneOnUnreachable=true")
        void create_defaultsShowPhoneTrue() {
            org.mockito.Mockito.when(notificationSettingRepository.save(any(NotificationSetting.class)))
                    .thenAnswer(i -> i.getArgument(0));
            service.create(99L);
            org.mockito.ArgumentCaptor<NotificationSetting> c =
                    org.mockito.ArgumentCaptor.forClass(NotificationSetting.class);
            org.mockito.Mockito.verify(notificationSettingRepository).save(c.capture());
            assertThat(c.getValue().getShowPhoneOnUnreachable()).isTrue();
        }
    }
```

- [ ] **Step 7: Тест репозитория** (`NotificationSettingRepositoryIntegrationTest`, новый @Nested): update персистит contact-поля.
```java
    @Nested
    @DisplayName("contacts")
    class Contacts {
        @Test
        @DisplayName("update сохраняет telegram/vk/max contact")
        void updatePersistsContacts() {
            notificationSettingRepository.save(NotificationSetting.builder()
                    .userId(userWithoutSettings).pushEnabled(true).callEnabled(false)
                    .telegramEnabled(false).active(true).showPhoneOnUnreachable(false).build());
            NotificationSetting s = notificationSettingRepository.findByUserId(userWithoutSettings);
            s.setTelegramContact("@ivan");
            s.setVkContact("ivan_vk");
            s.setMaxContact("max.ru/u/abc");
            notificationSettingRepository.update(s);
            NotificationSetting r = notificationSettingRepository.findByUserId(userWithoutSettings);
            assertThat(r.getTelegramContact()).isEqualTo("@ivan");
            assertThat(r.getVkContact()).isEqualTo("ivan_vk");
            assertThat(r.getMaxContact()).isEqualTo("max.ru/u/abc");
        }
    }
```
> Если Phase-1 тест `ShowPhoneOnUnreachable.defaultsFalseAndUpdatable` сохранял без `showPhoneOnUnreachable` — теперь `INSERT_NEW` требует его (NOT NULL). Задать `.showPhoneOnUnreachable(false)` в том тесте (как в BF5 Фаза 2 плане).

- [ ] **Step 8: Прогон + Commit**

Run: `cd backend && ./gradlew test --tests "ru.car.repository.NotificationSettingRepositoryIntegrationTest" --tests "ru.car.service.NotificationSettingServiceTest"`
```bash
git add backend/src/main/resources/db/ backend/src/main/java/ru/car/model/NotificationSetting.java backend/src/main/java/ru/car/repository/NotificationSettingRepository.java backend/src/main/java/ru/car/service/NotificationSettingService.java backend/src/test/java/ru/car/
git commit -m "feat(bf6): contact-поля notification_settings + дефолт-ON телефона для новых"
```

---

### Task A2: нормализация ссылок (ContactLinks) + owner_contacts в QrDto

**Files:**
- Create: `backend/src/main/java/ru/car/util/ContactLinks.java`
- Create: `backend/src/main/java/ru/car/dto/OwnerContactsDto.java`
- Modify: `backend/src/main/java/ru/car/dto/QrDto.java`
- Modify: `backend/src/main/java/ru/car/service/QrService.java`
- Test: `backend/src/test/java/ru/car/util/ContactLinksTest.java`
- Test: `backend/src/test/java/ru/car/service/QrServiceOwnerContactsTest.java`

**Interfaces:**
- Consumes: `NotificationSetting` contact-геттеры (A1), `notificationSettingRepository.findByQrId(UUID)` (null-safe), `userRepository.findById`.
- Produces: `QrDto.ownerContacts` (`OwnerContactsDto` или null); `ContactLinks.telegram/vk/max(String): String`.

- [ ] **Step 1: Падающий тест `ContactLinksTest`**
```java
package ru.car.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ContactLinksTest {
    @Test void telegram() {
        assertThat(ContactLinks.telegram("@ivan")).isEqualTo("https://t.me/ivan");
        assertThat(ContactLinks.telegram("ivan")).isEqualTo("https://t.me/ivan");
        assertThat(ContactLinks.telegram("https://t.me/ivan")).isEqualTo("https://t.me/ivan");
        assertThat(ContactLinks.telegram("  ")).isNull();
        assertThat(ContactLinks.telegram(null)).isNull();
    }
    @Test void vk() {
        assertThat(ContactLinks.vk("ivan_vk")).isEqualTo("https://vk.me/ivan_vk");
        assertThat(ContactLinks.vk("https://vk.com/ivan_vk")).isEqualTo("https://vk.me/ivan_vk");
        assertThat(ContactLinks.vk("id12345")).isEqualTo("https://vk.me/id12345");
    }
    @Test void max() {
        assertThat(ContactLinks.max("max.ru/u/abc")).isEqualTo("https://max.ru/u/abc");
        assertThat(ContactLinks.max("https://max.ru/u/abc")).isEqualTo("https://max.ru/u/abc");
        assertThat(ContactLinks.max("")).isNull();
    }
}
```

- [ ] **Step 2: Реализовать `ContactLinks`**
```java
package ru.car.util;

public final class ContactLinks {
    private ContactLinks() {}

    public static String telegram(String raw) {
        if (isBlank(raw)) return null;
        String h = raw.trim().replaceFirst("^@", "")
                .replaceFirst("(?i)^https?://(t\\.me|telegram\\.me)/", "");
        return h.isBlank() ? null : "https://t.me/" + h;
    }

    public static String vk(String raw) {
        if (isBlank(raw)) return null;
        String h = raw.trim().replaceFirst("(?i)^https?://(m\\.)?(vk\\.com|vk\\.me)/", "");
        return h.isBlank() ? null : "https://vk.me/" + h;
    }

    public static String max(String raw) {
        if (isBlank(raw)) return null;
        String r = raw.trim();
        return r.matches("(?i)^https?://.*") ? r : "https://" + r;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
```

- [ ] **Step 3: Запустить `ContactLinksTest` — PASS**

Run: `cd backend && ./gradlew test --tests "ru.car.util.ContactLinksTest"`

- [ ] **Step 4: `OwnerContactsDto`**
```java
package ru.car.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OwnerContactsDto {
    private String phone;
    private String telegram;
    private String vk;
    private String max;
}
```

- [ ] **Step 5: `QrDto`** — добавить поле (после `userId`):
```java
    @JsonProperty("owner_contacts")
    private OwnerContactsDto ownerContacts;
```

- [ ] **Step 6: Падающий тест `QrServiceOwnerContactsTest`**
```java
package ru.car.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.car.dto.QrDto;
import ru.car.mapper.QrWebDtoMapper;
import ru.car.model.NotificationSetting;
import ru.car.model.Qr;
import ru.car.model.User;
import ru.car.repository.NotificationSettingRepository;
import ru.car.repository.QrRepository;
import ru.car.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QrService owner_contacts")
class QrServiceOwnerContactsTest {

    @Mock QrRepository qrRepository;
    @Mock QrWebDtoMapper qrWebDtoMapper;
    @Mock NotificationSettingRepository notificationSettingRepository;
    @Mock UserRepository userRepository;
    // прочие зависимости QrService замокать по фактическому конструктору
    @Mock ru.car.mapper.QrDtoMapper qrDtoMapper;
    @Mock ru.car.repository.NotificationRepository notificationRepository;
    @Mock ru.car.service.security.AuthService authService;
    @Mock ru.car.service.MetricService metricService;

    @InjectMocks QrService qrService;

    @Test
    @DisplayName("отдаёт опубликованные контакты владельца")
    void returnsPublishedContacts() {
        UUID qrId = UUID.randomUUID();
        Qr qr = Qr.builder().id(qrId).userId(7L).build();
        when(qrRepository.findById(qrId)).thenReturn(Optional.of(qr));
        when(qrWebDtoMapper.toWebDto(qr)).thenReturn(QrDto.builder().qrId(qrId).build());
        when(notificationSettingRepository.findByQrId(qrId)).thenReturn(
                NotificationSetting.builder().userId(7L).showPhoneOnUnreachable(true)
                        .telegramContact("@ivan").vkContact(null).maxContact(null).build());
        when(userRepository.findById(7L)).thenReturn(Optional.of(
                User.builder().id(7L).phoneNumber("79991234567").build()));

        QrDto dto = qrService.getQrById(qrId);

        assertThat(dto.getOwnerContacts()).isNotNull();
        assertThat(dto.getOwnerContacts().getPhone()).isEqualTo("+79991234567");
        assertThat(dto.getOwnerContacts().getTelegram()).isEqualTo("https://t.me/ivan");
        assertThat(dto.getOwnerContacts().getVk()).isNull();
    }

    @Test
    @DisplayName("null-контакты, если владелец ничего не опубликовал")
    void nullWhenNothingPublished() {
        UUID qrId = UUID.randomUUID();
        Qr qr = Qr.builder().id(qrId).userId(7L).build();
        when(qrRepository.findById(qrId)).thenReturn(Optional.of(qr));
        when(qrWebDtoMapper.toWebDto(qr)).thenReturn(QrDto.builder().qrId(qrId).build());
        when(notificationSettingRepository.findByQrId(qrId)).thenReturn(
                NotificationSetting.builder().userId(7L).showPhoneOnUnreachable(false).build());

        assertThat(qrService.getQrById(qrId).getOwnerContacts()).isNull();
    }
}
```
> Сверь фактический конструктор `QrService` (`findByIdOrThrowNotFound` использует `qrRepository.findById`); добавь недостающие `@Mock` под реальные поля. `Qr`/`User` билдеры — проверь наличие `@Builder`.

- [ ] **Step 7: Запустить — FAIL** (нет `userRepository`/`notificationSettingRepository` в QrService, нет наполнения contacts)

- [ ] **Step 8: `QrService`** — добавить зависимости и наполнение

8a. Импорты + поля:
```java
import ru.car.dto.OwnerContactsDto;
import ru.car.model.NotificationSetting;
import ru.car.model.User;
import ru.car.repository.NotificationSettingRepository;
import ru.car.repository.UserRepository;
import ru.car.util.ContactLinks;
```
```java
    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;
```
8b. Заменить `getQrById(UUID)`:
```java
    @Transactional
    public QrDto getQrById(UUID id) {
        Qr qr = findByIdOrThrowNotFound(id);
        QrDto dto = qrWebDtoMapper.toWebDto(qr);
        dto.setOwnerContacts(resolveOwnerContacts(qr));
        return dto;
    }

    /** BF6: опубликованные владельцем контакты для развилки на скане (null, если ничего). */
    private OwnerContactsDto resolveOwnerContacts(Qr qr) {
        if (qr.getUserId() == null) {
            return null;
        }
        NotificationSetting setting = notificationSettingRepository.findByQrId(qr.getId());
        if (setting == null) {
            return null;
        }
        String phone = null;
        if (Boolean.TRUE.equals(setting.getShowPhoneOnUnreachable())) {
            phone = userRepository.findById(setting.getUserId())
                    .map(User::getPhoneNumber)
                    .filter(p -> p != null && !p.isBlank())
                    .map(p -> "+" + p)
                    .orElse(null);
        }
        OwnerContactsDto contacts = OwnerContactsDto.builder()
                .phone(phone)
                .telegram(ContactLinks.telegram(setting.getTelegramContact()))
                .vk(ContactLinks.vk(setting.getVkContact()))
                .max(ContactLinks.max(setting.getMaxContact()))
                .build();
        boolean empty = contacts.getPhone() == null && contacts.getTelegram() == null
                && contacts.getVk() == null && contacts.getMax() == null;
        return empty ? null : contacts;
    }
```

- [ ] **Step 9: Запустить — PASS** (`ContactLinksTest`, `QrServiceOwnerContactsTest`)

- [ ] **Step 10: Commit**
```bash
git add backend/src/main/java/ru/car/util/ContactLinks.java backend/src/main/java/ru/car/dto/OwnerContactsDto.java backend/src/main/java/ru/car/dto/QrDto.java backend/src/main/java/ru/car/service/QrService.java backend/src/test/java/ru/car/util/ContactLinksTest.java backend/src/test/java/ru/car/service/QrServiceOwnerContactsTest.java
git commit -m "feat(bf6): owner_contacts в GET /api/qr/{id} (готовые ссылки TG/VK/MAX + телефон)"
```

---

### Task A3: настройки принимают contact-поля (DTO + mapper)

**Files:**
- Modify: `backend/src/main/java/ru/car/dto/NotificationSettingDto.java`
- Modify: `backend/src/main/java/ru/car/mapper/NotificationSettingDtoMapper.java`
- Test: `backend/src/test/java/ru/car/mapper/NotificationSettingDtoMapperTest.java`

- [ ] **Step 1: DTO** — добавить поля:
```java
    @JsonProperty("telegram_contact")
    private String telegramContact;
    @JsonProperty("vk_contact")
    private String vkContact;
    @JsonProperty("max_contact")
    private String maxContact;
```

- [ ] **Step 2: Mapper `updateIgnoreNull`** — добавить три `@Mapping` (IGNORE), чтобы null-поля не затирали:
```java
    @Mapping(target = "telegramContact", source = "telegramContact", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "vkContact", source = "vkContact", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "maxContact", source = "maxContact", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
```

- [ ] **Step 3: Тест маппера** — добавить в `NotificationSettingDtoMapperTest`:
```java
    @Test
    @DisplayName("updateIgnoreNull копирует contact-поля")
    void updateIgnoreNullCopiesContacts() {
        NotificationSetting setting = NotificationSetting.builder().build();
        NotificationSettingDto dto = NotificationSettingDto.builder()
                .telegramContact("@ivan").vkContact("ivan_vk").maxContact("max.ru/u/abc").build();
        mapper.updateIgnoreNull(dto, setting);
        assertThat(setting.getTelegramContact()).isEqualTo("@ivan");
        assertThat(setting.getVkContact()).isEqualTo("ivan_vk");
        assertThat(setting.getMaxContact()).isEqualTo("max.ru/u/abc");
    }
```

- [ ] **Step 4: Прогон + Commit**

Run: `cd backend && ./gradlew test --tests "ru.car.mapper.NotificationSettingDtoMapperTest"`
```bash
git add backend/src/main/java/ru/car/dto/NotificationSettingDto.java backend/src/main/java/ru/car/mapper/NotificationSettingDtoMapper.java backend/src/test/java/ru/car/mapper/NotificationSettingDtoMapperTest.java
git commit -m "feat(bf6): notification_settings.patch принимает telegram/vk/max contact"
```

### Task A4: полный backend-прогон

- [ ] `cd backend && ./gradlew test` → BUILD SUCCESSFUL.

---

## Фаза B — Web (развилка на qr.html)

### Task B1: кнопки-развилка

**Files:**
- Modify: `frontend/qr.html`
- Modify: `frontend/js/script.js`
- Modify: `frontend/css/style.css`

- [ ] **Step 1: `qr.html`** — добавить контейнер развилки (в секцию, где сейчас `#about`, перед download-app):
```html
<section id="qr-actions" class="qr-actions" hidden></section>
```

- [ ] **Step 2: `script.js`** — в `checkQr()` заменить авто-вызов `createMsg()` на рендер развилки:
```javascript
            if (response.status === "ACTIVE" || response.status === "TEMPORARY") {
                renderActions(response.owner_contacts);
                return;
            }
```

- [ ] **Step 3: `script.js`** — функция рендера (рядом с `createMsg`)
```javascript
// BF6: развилка действий после скана — сообщить о событии / написать / позвонить.
function renderActions(contacts) {
    const box = document.getElementById("qr-actions");
    if (!box) { createMsg(); return; }

    const buttons = [];
    buttons.push(actionButton("Сообщить о событии", null, "primary", createMsg));
    if (contacts) {
        if (contacts.telegram) buttons.push(actionButton("Написать в Telegram", contacts.telegram));
        if (contacts.vk) buttons.push(actionButton("Написать в VK", contacts.vk));
        if (contacts.max) buttons.push(actionButton("Написать в MAX", contacts.max));
        if (contacts.phone) buttons.push(actionButton("Позвонить", "tel:" + contacts.phone));
    }
    box.replaceChildren(...buttons);
    box.hidden = false;
}

function actionButton(text, href, variant, onClick) {
    const el = href ? document.createElement("a") : document.createElement("button");
    el.className = "qr-action-btn" + (variant === "primary" ? " qr-action-primary" : "");
    el.textContent = text;
    if (href) { el.href = href; if (href.startsWith("http")) el.target = "_blank"; }
    if (onClick) el.addEventListener("click", (e) => { e.preventDefault(); onClick(); });
    return el;
}
```

- [ ] **Step 4: `style.css`** — стили (по образцу `.owner-call-button`)
```css
/* BF6: развилка действий на странице метки */
.qr-actions { display: flex; flex-direction: column; gap: 12px; padding: 20px; max-width: 420px; margin: 0 auto; }
.qr-action-btn {
    display: block; text-align: center; text-decoration: none;
    padding: 16px 20px; border-radius: 24px; font-weight: 600; font-size: 1.06rem;
    border: 2px solid var(--orange-button); color: var(--orange-button); background: #fff; cursor: pointer;
}
.qr-action-primary { background: var(--orange-button); color: #fff; }
```

- [ ] **Step 5: Ручная проверка**

Backend локально + `cd frontend && python3 -m http.server 8000`. У тестового владельца задать контакты (patch) → открыть `/qr/{uuid}` → видны кнопки; «Сообщить о событии» ведёт в текущий флоу выбора причины; мессенджеры/звонок открывают ссылки. Метка NEW/DELETED → сообщение как раньше.

- [ ] **Step 6: Commit**
```bash
git add frontend/qr.html frontend/js/script.js frontend/css/style.css
git commit -m "feat(bf6): развилка действий (сообщить/написать/позвонить) на странице метки"
```

---

## Фаза C — Mobile (поля контактов)

### Task C1: модель + контроллер + экран + версия

**Files:**
- Modify: `mobile/lib/models/notification_settings.dart`
- Modify: `mobile/lib/controllers/notification_settings_controller.dart`
- Modify: `mobile/lib/screens/notifications_screen/notification_settings_screen.dart`
- Modify: `mobile/pubspec.yaml` (version)
- Test: `mobile/test/notification_settings_test.dart`

- [ ] **Step 1: Тест модели** `mobile/test/notification_settings_test.dart`
```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:app/models/notification_settings.dart';

void main() {
  test('fromJson parses contacts + show_phone', () {
    final s = NotificationSettings.fromJson({
      'push_enabled': true, 'call_enabled': false, 'telegram_enabled': false,
      'show_phone_on_unreachable': true,
      'telegram_contact': '@ivan', 'vk_contact': 'ivan_vk', 'max_contact': 'max.ru/u/abc',
    });
    expect(s.showPhoneOnUnreachable, true);
    expect(s.telegramContact, '@ivan');
    expect(s.vkContact, 'ivan_vk');
    expect(s.maxContact, 'max.ru/u/abc');
  });
}
```

- [ ] **Step 2: Модель** — добавить поля (`showPhoneOnUnreachable`, `telegramContact`, `vkContact`, `maxContact`) в конструктор и `fromJson` (по образцу существующих; для строк — `json['x'] is String ? json['x'] : null`, для bool — как push_enabled).

- [ ] **Step 3: Запустить `flutter test test/notification_settings_test.dart` — PASS**

- [ ] **Step 4: Контроллер** — в `paramsNames`: `'show_phone': 'show_phone_on_unreachable'`, `'telegram_contact': 'telegram_contact'`, `'vk_contact': 'vk_contact'`, `'max_contact': 'max_contact'`. Локальные поля + `mapSettings()`. Для текстовых полей — метод сохранения по потере фокуса/кнопке «Сохранить», вызывающий `patchNotificationSettings({'telegram_contact': value})`.

- [ ] **Step 5: Экран** — добавить: тумблер «Показывать мой номер прохожему» (`show_phone`) + 3 текстовых поля (Telegram `@username`, VK ссылка/id, MAX — вставить ссылку) с подсказками. Стиль — по образцу существующих виджетов экрана.

- [ ] **Step 6: Версия** — `mobile/pubspec.yaml`: `version: 1.1.5+45`.

- [ ] **Step 7: `flutter analyze` + `flutter test`** (если тулчейн доступен; иначе — при сборке владельцем).

- [ ] **Step 8: Commit**
```bash
git add mobile/
git commit -m "feat(bf6): контакты владельца (телефон-тумблер + Telegram/VK/MAX) в настройках, bump 1.1.5+45"
```

---

## Self-Review

**Spec coverage:** contact-поля + дефолт-ON телефона → A1; owner_contacts в qr-эндпоинте + нормализация → A2; патч настроек принимает контакты → A3; развилка на скане → B1; мобильные поля + версия → C1; согласие/Политика и публикация в сторы — вне кода (владелец, отмечено). Покрыто.

**Placeholder scan:** backend-код полный (A1-A3); Web (B1) — полный JS/CSS; Mobile (C1) Steps 2/4/5 описаны «по образцу существующих» (модель/контроллер/экран) — это осознанно, т.к. точная вёрстка GetX-экрана объёмна; ключевой код (тест, поля, paramsNames) приведён.

**Type consistency:** `showPhoneOnUnreachable`, `telegramContact/vkContact/maxContact` — единые имена в модели/DTO/SQL/mapper (backend) и Dart; JSON-ключи `telegram_contact/vk_contact/max_contact`, `show_phone_on_unreachable`, `owner_contacts`. `ContactLinks.telegram/vk/max`. `OwnerContactsDto{phone,telegram,vk,max}`.

**Open verification points:** конструктор `QrService` (полный список `@Mock` в A2/Step6); Phase-1 репо-тест про NOT NULL show_phone (A1/Step7); наличие `flutter` в окружении (C1); точные виджеты GetX-экрана (C1/Step5).

## Не входит
- Обновление Политики/Оферты (владелец, до деплоя дефолт-ON телефона).
- Валидация ввода контактов от мусора/чужих ссылок (можно добавить позже).
- Telegram-бот управление контактами (мобайл — основной клиент).
- Сборка и публикация в RuStore/App Store (владелец).
