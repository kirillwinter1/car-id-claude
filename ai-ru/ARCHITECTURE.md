# Архитектура Car-ID

> Карта кодовой базы. Функциональность — в [FEATURES.md](FEATURES.md), техдолг — в [TECH_DEBT.md](TECH_DEBT.md).

## Обзор

Монорепозиторий с тремя компонентами:

- **backend/** — Spring Boot 3.2.3 (Java 17).
- **mobile/** — Flutter (Dart 3.1+), GetX.
- **frontend/** — статический сайт, vanilla HTML/CSS/JS.

---

## Backend

### Стек

| Слой | Технология |
|------|-----------|
| Framework | Spring Boot 3.2.3, Java 17 |
| БД | PostgreSQL (prod), H2 in-memory (dev/test) |
| Миграции | Liquibase (`src/main/resources/db/master.xml` + `changelog/`) |
| Data access | `NamedParameterJdbcTemplate` (без JPA/Hibernate) |
| Security | Spring Security + JWT (JJWT 0.12.3) |
| Шифрование конфига | Jasypt 3.0.5 (prod-секреты через `ENC(...)`) |
| API docs | SpringDoc OpenAPI 2.4.0 |
| Метрики | Micrometer + Prometheus через Actuator |
| Маппинг | MapStruct 1.5.5 |

### Пакетная структура `ru.car`

```
ru.car/
├── CarIdApplication.java               entry point
├── config/
│   ├── Config.java                     ExecutorService, Jasypt, Scheduling
│   └── security/                       Spring Security, JWT filter
├── controller/                         REST-контроллеры (mobile + web + callbacks)
├── service/
│   ├── *.java                          Facade + бизнес-логика фич
│   ├── security/                       Auth, JWT, UserDetails
│   └── message/                        Каналы доставки (Sender-интерфейс)
│       ├── telegram/                   F8
│       ├── firebase/                   F7
│       ├── zvonok/                     F11
│       ├── sms/                        F10
│       └── mail/                       (не используется — см. TECH_DEBT D1)
├── repository/                         JdbcTemplate-репозитории
├── model/                              Entity POJO
├── dto/                                Request/Response DTO (mobile/web)
├── mapper/                             MapStruct
├── enums/
├── exception/                          Централизованный RestExceptionHandler
├── scheduler/                          TemporaryQrScheduler (F3)
├── monitoring/                         Events + MonitoringService (F16)
├── util/                               QR/SVG/PDF генерация (см. OPERATIONS.md)
└── filter/
```

### Какой код отвечает за какую фичу

Соответствие пакет ↔ фича — см. «Реализация» в карточках [features/](features/).

| Фича | Основные классы |
|------|-----------------|
| [F1](features/F1_AUTH.md) Auth | `LoginAuthMobile*`, `AuthenticationCodeService`, `security/Jwt*`, `security/Auth*` |
| [F2](features/F2_QR_CODES.md) QR | `QrController`, `QrWebController`, `QrService`, `QrRepository` |
| [F3](features/F3_TEMPORARY_QR.md) Temp QR | `QrService.createTemporaryQr`, `TemporaryQrScheduler` |
| [F4](features/F4_WEB_REPORT.md) Web report | `ReportWebController`, `NotificationFacade`, `NotificationService` |
| [F5](features/F5_NOTIFICATIONS.md) Notifications | `Notification*Controller`, `ReportController`, `NotificationService` |
| [F6](features/F6_NOTIFICATION_SETTINGS.md) Settings | `NotificationSettingController`, `NotificationSettingService` |
| [F7](features/F7_FIREBASE_PUSH.md) Push | `service/message/firebase/`, `FirebaseTokenService` |
| [F8](features/F8_TELEGRAM_BOT.md) Telegram | `service/message/telegram/` |
| [F10](features/F10_SMS.md) SMS | `service/message/sms/` |
| [F11](features/F11_ZVONOK.md) Zvonok | `service/message/zvonok/`, `ZvonokPostBackController` |
| [F12](features/F12_FEEDBACK.md) Feedback | `FeedbackController`, `FeedbackService`, `MessageService.sendMail` |
| [F13](features/F13_REASONS.md) Reasons | `ReasonDictionaryService` |
| [F14](features/F14_MARKETPLACES.md) Marketplaces | `MarketplacesController`, `MarketplaceService` |
| [F15](features/F15_VERSION_CONTROL.md) Version | `VersionControlController`, `VersionControlService` |
| [F16](features/F16_METRICS_MONITORING.md) Metrics | `MetricService`, `monitoring/` |

### Ключевые enum'ы

| Enum | Значения | Где используется |
|------|----------|-----------------|
| `Role` | `ROLE_USER`, `ROLE_ADMIN` | `User.role` |
| `QrStatus` | `NEW`, `ACTIVE`, `TEMPORARY`, `DELETED` | `Qr.status` |
| `NotificationStatus` | `DRAFT`, `SEND`, `UNREAD`, `READ` | `Notification.status` |
| `FeedbackChannels` | `APP`, `WEB`, `TELEGRAM`*  | `Feedback.channel` (*`TELEGRAM` не используется) |
| `BatchTemplates` | набор шаблонов стикеров | `Batch.template` |
| `DistributionMethods` | методы распространения | `Batch.distribution_method` |
| `Events` | `REGISTER_USER`, `ACTIVATE_QR`, `SEND_NOTIFICATION`, `READ_NOTIFICATION` | `monitoring_repository` |
| `ErrorCode` | машинные коды ошибок | `RestExceptionHandler` |

### Entity и связи

```
User (users)
  id, phone_number, role, active, created_date

  ├─ NotificationSetting (notification_settings) 1:1
  │    user_id, push_enabled, call_enabled, telegram_enabled,
  │    active, telegram_dialog_id
  │
  ├─ FirebaseToken (firebase_token) 1:N
  │    auth_id, user_id, token
  │
  ├─ AuthenticationCode (authentication_code) 1:N
  │    phone_number, code, created_date
  │
  ├─ Qr (qrs) 1:N
  │    id (UUID), user_id, batch_id, status, name, seq_number,
  │    printed, created_date
  │
  │    ├─ Notification (notification) 1:N (FK qr_id → qrs.id)
  │    │    id (UUID), qr_id, reason_id, sender_id, visitor_id,
  │    │    text, status, call_id, created_date, read_date
  │    │
  │    └─ ReasonDictionary (reason_dictionary) FK reason_id
  │
  └─ Feedback (feedback) 1:N
       user_id (nullable для web), email, text, channel

Batch (qr_batches)           singleton-row таблицы:
  id, template,              - Marketplaces (marketplaces): id, wb, ozon, activity
  distribution_method,       - VersionControl (version_control): 6 полей (apple/google/rustore × current/min)
  description
```

### REST API

Полный список endpoints — в карточках фич. Краткая группировка:

| Префикс | Назначение | Аутентификация |
|---------|-----------|----------------|
| `/api/user.*`, `/api/user.login*` | Auth + профиль | частично JWT |
| `/api/qr.*` | Управление QR (mobile) | JWT |
| `/api/qr/{id}` | Публичные данные QR | нет |
| `/api/notification.*` | Входящие (mobile) | JWT |
| `/api/notification/{id}/status` | Публичный статус | нет |
| `/api/report.*` | Отправка и listing (mobile) | JWT |
| `/api/report/*` (dash-path) | Web-endpoints | нет |
| `/api/notification_settings.*` | Каналы, firebase token | JWT |
| `/api/feedback.*` / `/api/feedback/*` | Support (mobile + web) | mobile JWT, web без auth |
| `/api/marketplaces.get` | Ссылки на магазины | нет |
| `/api/version_control.get` | Версии клиента | нет |
| `/api/zvonok/*` | Callbacks от Zvonok | **нет** (см. [TECH_DEBT.md S7](TECH_DEBT.md)) |
| `/actuator/*` | Health, metrics, prometheus | нет (см. [TECH_DEBT.md S8, O5](TECH_DEBT.md)) |

### Асинхронность

- `ExecutorService pushExecutorService` (`config/Config.java`) — 8 потоков, используется для веерной отправки уведомлений и загрузки PDF в Telegram.
- `CompletableFuture.delayedExecutor(30s, ...)` — отложенные действия (SMS-резерв в auth, Zvonok-дозвон, hello push новому пользователю).
- `@Scheduled` — `TemporaryQrScheduler` (`fixedDelay = 2h`).

### Интерфейс `Sender`

```java
interface Sender {
  String getServiceName();
  boolean sendNotification(TextMessage message);
  boolean canSendNotification(NotificationSetting setting);
}
```

Реализации: `FirebaseService` ([F7](features/F7_FIREBASE_PUSH.md)), `TelegramBotService` ([F8](features/F8_TELEGRAM_BOT.md)), `ZvonokService` ([F11](features/F11_ZVONOK.md)), `SmsService` ([F10](features/F10_SMS.md) — не используется в веере, только для резерва auth).

---

## Mobile (Flutter)

### Стек

| Слой | Технология |
|------|-----------|
| SDK | Dart 3.1+ |
| State | GetX 4.6.6 (DI + routing + state в одном пакете) |
| HTTP | `GetConnect` |
| Local storage | `GetStorage` |
| Push | `firebase_messaging` 14.6.3 |
| QR scan | `qr_code_scanner` 1.0.1 |
| Аналитика | `firebase_analytics` 10.4.3, `firebase_crashlytics` 3.3.3 |

### Структура `lib/`

```
lib/
├── main.dart                           Firebase init, app entry
├── bindings/default_binding.dart       GetX DI
├── controllers/                        GetX-контроллеры
│   ├── app_controller.dart
│   ├── auth_controller.dart
│   ├── home_controller.dart
│   ├── splash_controller.dart
│   ├── qr_scan_controller.dart
│   ├── qrmarks_controller.dart
│   ├── notifications_controller.dart
│   ├── push_notification_controller.dart
│   ├── qr_report_event_controller.dart
│   ├── feedback_controller.dart
│   └── notification_settings_controller.dart
├── models/                             Data classes + fromJson/toJson
├── repository/                         API-клиенты (GetConnect)
├── screens/                            Экраны по фичам
├── widgets/                            Переиспользуемые UI-элементы
└── utils/
    ├── const.dart                      URL, таймауты, константы
    ├── routes.dart                     GetX маршруты
    ├── theme.dart
    └── i18n/                           только ru_RU (см. TECH_DEBT M2)
```

### Экраны и маршруты

| Путь | Экран | Назначение |
|------|-------|-----------|
| `/` | SplashScreen | инициализация, version check ([F15](features/F15_VERSION_CONTROL.md)) |
| `/auth` | AuthScreen | ввод телефона + кода ([F1](features/F1_AUTH.md)) |
| `/home` | HomeScreen | хаб |
| `/qr_marks` | QRMarksScreen | список QR пользователя ([F2](features/F2_QR_CODES.md)) |
| `/qr_scan` | QrScanScreen | сканирование для привязки |
| `/notifications` | NotificationsScreen | входящие/исходящие ([F5](features/F5_NOTIFICATIONS.md)) |
| `/notification_settings` | NotificationSettingsScreen | каналы ([F6](features/F6_NOTIFICATION_SETTINGS.md)) |
| `/qr_add_event` | QRReportEventScreen | отправка события ([F4](features/F4_WEB_REPORT.md) mobile-вариант) |
| `/select_event` | SelectEventScreen | выбор причины ([F13](features/F13_REASONS.md)) |

---

## Frontend (static)

Статический сайт, **без сборки** (vanilla JS + ES6 modules + Fetch API).

```
frontend/
├── index.html              лендинг
├── qr.html                 точка входа при сканировании
├── notification.html       форма выбора причины и отправки
├── offer.html              публичная оферта
├── privacy-policy.html     политика конфиденциальности
├── css/                    reset.css, style.css, svg-bg.css
└── js/
    ├── script.js           checkQr → createDraft
    ├── sendMsg.js          список причин + updateDraft/send
    └── systemMessages.js   тексты модальных окон
```

Соответствие страниц фичам:
- `qr.html` + `script.js` → [F2](features/F2_QR_CODES.md) (публичный GET) + [F4](features/F4_WEB_REPORT.md) (создание draft).
- `notification.html` + `sendMsg.js` → [F4](features/F4_WEB_REPORT.md) + [F13](features/F13_REASONS.md) (причины).
- Статусы — [F5](features/F5_NOTIFICATIONS.md) (публичный endpoint).

---

## Потоки данных (коротко)

### Auth
```
Mobile → POST /api/user.login_oauth_mobile → Zvonok flashcall → (30 сек → SMS fallback)
Mobile → POST /api/user.login_oauth_code   → JWT
```

### Отправка с веба
```
Прохожий сканирует QR → qr.html → GET /api/qr/{id}
→ POST /api/report/createDraft      → Notification(DRAFT)
→ PUT  /api/report/updateDraft SEND → Notification(UNREAD)
→ MessageService.asyncSend → [Firebase, Telegram] → (30s → Zvonok, если callEnabled)
```

### Получение на mobile
```
Backend → FCM → push_notification_controller → NotificationsScreen
              ↓
        (Telegram / Zvonok — опционально, по настройкам F6)
```

## Ссылки

- [FEATURES.md](FEATURES.md) — функциональность по фичам.
- [TECH_DEBT.md](TECH_DEBT.md) — технический долг.
- [OPERATIONS.md](OPERATIONS.md) — операционный runbook.
- [RULES.md](RULES.md) — бизнес-правила + dev-стандарты.
