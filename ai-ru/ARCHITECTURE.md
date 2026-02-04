# Архитектура Car-ID

## Обзор

Car-ID — сервис QR-меток для автомобилей. Монорепозиторий с тремя компонентами:
- **Backend** — Spring Boot 3.2.3 (Java 17)
- **Mobile** — Flutter (Dart 3.1+)
- **Frontend** — Vanilla HTML/CSS/JS

---

## Backend (Spring Boot 3, Java 17)

### Технологический стек

| Компонент | Технология |
|-----------|------------|
| Framework | Spring Boot 3.2.3 |
| Java | 17 |
| Database | PostgreSQL (prod), H2 (dev) |
| Migrations | Liquibase |
| Data Access | JdbcTemplate (не JPA!) |
| Security | Spring Security + JWT (JJWT 0.12.3) |
| Config Encryption | Jasypt Spring Boot 3.0.5 |
| API Docs | SpringDoc OpenAPI 2.4.0 |
| Monitoring | Micrometer + Prometheus |

### Пакетная структура

```
ru.car/
├── CarIdApplication.java       — Entry point
├── config/
│   ├── Config.java             — ExecutorService, Jasypt, Scheduling
│   └── security/
│       ├── SecurityConfiguration.java — Spring Security, JWT filter
│       ├── JwtAuthenticationFilter.java
│       └── RestAuthenticationEntryPoint.java
├── controller/                 — 13 REST контроллеров
│   ├── UserController          — Профиль пользователя
│   ├── QrController            — Управление QR-метками
│   ├── LoginAuthMobileController — Аутентификация (flashcall + код)
│   ├── NotificationController  — Входящие уведомления
│   ├── ReportController        — Отправка уведомлений
│   ├── FeedbackController      — Обратная связь
│   ├── NotificationSettingController — Настройки уведомлений
│   ├── VersionControlController — Версии приложения
│   ├── MarketplacesController  — Справочник маркетплейсов
│   ├── QrWebController         — Web API для QR
│   ├── NotificationWebController — Web API статус уведомления
│   ├── ReportWebController     — Web API отчёты
│   └── ZvonokPostBackController — Webhook от Zvonok
├── service/                    — Бизнес-логика
│   ├── UserService             — 74 LOC
│   ├── QrService               — 147 LOC
│   ├── NotificationService     — 221 LOC
│   ├── NotificationFacade
│   ├── NotificationSettingService — 85 LOC
│   ├── LoginAuthMobileService  — 79 LOC
│   ├── AuthenticationCodeService
│   ├── FeedbackService
│   ├── MarketplaceService
│   ├── ReasonDictionaryService
│   ├── VersionControlService
│   ├── FirebaseTokenService
│   ├── MetricService
│   ├── security/
│   │   ├── AuthService         — Текущий пользователь
│   │   ├── JwtService          — 135 LOC (JWT генерация/валидация)
│   │   └── SecurityUserService — UserDetailsService
│   └── message/                — Интеграции для отправки
│       ├── MessageService      — 221 LOC (фасад для всех каналов)
│       ├── telegram/
│       │   ├── TelegramBotService — 192 LOC
│       │   ├── TelegramLogicService — 144 LOC
│       │   └── TelegramMenu    — 91 LOC
│       ├── firebase/
│       │   └── FirebaseService — 133 LOC (push)
│       ├── sms/
│       │   └── SmsService      — SMS через SMS Aero
│       ├── zvonok/
│       │   └── ZvonokService   — 130 LOC (flashcall + voice)
│       ├── whatsapp/
│       │   └── WhatsappBotService — Green API
│       └── mail/
│           └── MailSender      — Email
├── repository/                 — JdbcTemplate репозитории
│   ├── UserRepository
│   ├── QrRepository            — 157 LOC
│   ├── NotificationRepository  — 277 LOC
│   ├── AuthenticationCodeRepository — 85 LOC
│   ├── FirebaseTokenRepository — 91 LOC
│   ├── NotificationSettingRepository
│   ├── FeedbackRepository
│   ├── BatchRepository
│   ├── VersionControlRepository
│   ├── ReasonDictionaryRepository
│   └── MarketplacesRepository
├── model/                      — 11 Entity классов
│   ├── User
│   ├── Qr
│   ├── Batch
│   ├── Notification
│   ├── NotificationSetting
│   ├── AuthenticationCode
│   ├── Feedback
│   ├── FirebaseToken
│   ├── ReasonDictionary
│   ├── VersionControl
│   └── Marketplaces
├── dto/                        — 46 DTO классов
│   ├── mobile/requestTypes/
│   ├── mobile/responseTypes/
│   ├── web/requestTypes/
│   ├── login_oauth_code/
│   └── login_auth_mobile/
├── mapper/                     — 10 MapStruct маперов
├── enums/
│   ├── Role                    — ROLE_USER, ROLE_ADMIN
│   ├── QrStatus                — NEW, ACTIVE, TEMPORARY, DELETED
│   ├── NotificationStatus      — SEND, DRAFT, UNREAD, READ
│   ├── ErrorCode
│   ├── BatchTemplates
│   ├── DistributionMethods
│   └── FeedbackChannels
├── exception/                  — Обработка ошибок
│   ├── RestExceptionHandler    — 99 LOC (централизованная обработка)
│   ├── NotFoundException
│   ├── ForbiddenException
│   ├── UnauthorizedException
│   └── BadRequestException
├── scheduler/
│   └── TemporaryQrScheduler    — Удаление временных QR каждые 2 часа
├── monitoring/                 — Кастомные метрики
│   ├── MonitoringService
│   ├── MonitoringBeanPostProcessor
│   └── @Monitoring annotation
├── util/
│   ├── QrUtils                 — 308 LOC (генерация QR)
│   ├── SvgUtils                — 261 LOC (SVG трансформации)
│   └── svg/
│       ├── Matrix.java         — 308 LOC
│       └── PdfConverter.java   — 137 LOC (SVG → PDF)
└── filter/
    └── RequestURIOverriderServletFilter
```

### Ключевые сервисы

| Сервис | LOC | Ответственность |
|--------|-----|-----------------|
| NotificationRepository | 277 | Сложный поиск и фильтрация уведомлений |
| QrUtils | 308 | Генерация QR-кодов, SVG/PDF |
| Matrix | 308 | Матрицы для QR |
| SmsAeroClient | 269 | SMS Aero интеграция |
| SvgUtils | 261 | SVG трансформации |
| NotificationService | 221 | Основная логика уведомлений |
| MessageService | 221 | Фасад для всех каналов отправки |
| TelegramBotService | 192 | Telegram интеграция |
| QrRepository | 157 | Поиск, статусы QR |
| QrService | 147 | CRUD QR-меток |
| PdfConverter | 137 | SVG → PDF |
| JwtService | 135 | JWT генерация/валидация |
| FirebaseService | 133 | Push notifications |
| ZvonokService | 130 | Flashcall/Voice calls |

### Entity и связи

```
┌─────────────────────────────────────────────────┐
│ User                                            │
│ - id, phoneNumber, role, active                 │
│ - telegramChatId (для Telegram уведомлений)     │
└────────┬────────────────────────────────────────┘
         │ 1:N
         ├─→ Qr (QR-метки)
         │   - id (UUID), batchId (FK → Batch)
         │   - status: NEW, ACTIVE, TEMPORARY, DELETED
         │   - seqNumber, qrName
         │   └─→ 1:N Notification
         │       - id (UUID), qrId (FK)
         │       - reasonId (FK → ReasonDictionary)
         │       - senderId (FK → User, nullable)
         │       - visitorId (гостевой ID)
         │       - status: DRAFT, SEND, UNREAD, READ
         │
         ├─→ NotificationSetting
         │   - pushEnabled, callEnabled
         │   - telegramEnabled, whatsappEnabled
         │   - telegramDialogId
         │
         ├─→ FirebaseToken
         │   - authId, token
         │
         ├─→ AuthenticationCode
         │   - phoneNumber, code, smsId
         │
         └─→ Feedback
             - channel, email, text

┌─────────────────────────────────────┐
│ Batch (партии QR)                   │
│ - id, template, distributionMethod  │
│ └─→ 1:N Qr                          │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ ReasonDictionary                    │
│ - id, description                   │
│ └─→ 1:N Notification                │
└─────────────────────────────────────┘
```

### API Endpoints

#### Аутентификация (`/api`)
| Endpoint | Метод | Описание |
|----------|-------|----------|
| `/api/user.login_oauth_mobile` | POST | Запросить код верификации |
| `/api/user.login_oauth_code` | POST | Подтвердить код → JWT |
| `/api/user.logout` | POST | Logout |
| `/api/user.get` | POST | Получить профиль |
| `/api/user.delete` | POST | Удалить профиль |

#### QR-метки (`/api`)
| Endpoint | Метод | Описание |
|----------|-------|----------|
| `/api/qr.get` | POST | Информация о QR |
| `/api/qr.create` | POST | Создать QR |
| `/api/qr.create_temporary` | POST | Создать временный QR |
| `/api/qr.link_to_user` | POST | Привязать QR к пользователю |
| `/api/qr.get_all` | POST | Все QR пользователя |
| `/api/qr.delete` | POST | Удалить QR |

#### Уведомления (`/api`)
| Endpoint | Метод | Описание |
|----------|-------|----------|
| `/api/notification.get_all` | POST | Все входящие (пагинация) |
| `/api/notification.get_all_unread` | POST | Непрочитанные |
| `/api/notification.mark_as_read` | POST | Отметить прочитанным |
| `/api/report.get_all` | POST | Все исходящие |
| `/api/report.send` | POST | Отправить уведомление |
| `/api/report.get_all_reasons` | POST | Справочник причин |

#### Web API (`/api/web`)
| Endpoint | Метод | Описание |
|----------|-------|----------|
| `/api/web/qr/{id}` | GET | Информация о QR |
| `/api/web/notification/{id}/status` | GET | Статус уведомления |

---

## Mobile (Flutter)

### Технологический стек

| Компонент | Технология |
|-----------|------------|
| SDK | Dart >=3.1.0 <4.0.0 |
| State Management | GetX 4.6.6 |
| HTTP Client | GetConnect |
| Local Storage | GetStorage |
| Push | Firebase Messaging 14.6.3 |
| QR Scanner | qr_code_scanner 1.0.1 |
| Analytics | Firebase Analytics 10.4.3 |
| Crashlytics | Firebase Crashlytics 3.3.3 |

### Структура папок

```
lib/
├── main.dart                   — Entry point, Firebase init
├── bindings/
│   └── default_binding.dart    — GetX DI
├── controllers/                — GetX контроллеры
│   ├── app_controller.dart     — Главный (user, token, marketplaces)
│   ├── app_event.dart          — Login/Logout/AppStart события
│   ├── auth_controller.dart    — Авторизация (телефон + SMS)
│   ├── home_controller.dart    — Главный экран
│   ├── splash_controller.dart  — Инициализация, проверка версии
│   ├── qr_scan_controller.dart — Сканирование QR для добавления
│   ├── qrmarks_controller.dart — Список QR-меток
│   ├── notifications_controller.dart — Входящие/исходящие
│   ├── push_notification_controller.dart — FCM + локальные пуши
│   ├── qr_report_event_controller.dart — Отправка события
│   ├── feedback_controller.dart
│   └── notification_settings_controller.dart
├── models/
│   ├── user.dart               — {id, phoneNumber}
│   ├── qr_mark.dart            — QR-метка (основная сущность)
│   ├── car_notification.dart   — Уведомление
│   ├── report_event.dart       — Тип события
│   ├── notification_settings.dart
│   ├── marketplaces.dart
│   ├── request_answer.dart
│   └── app_version.dart
├── repository/                 — API клиенты (GetConnect)
│   ├── UserRepository.dart
│   ├── QRMarksRepository.dart
│   ├── NotificationsRepository.dart
│   ├── QRReportEventRepository.dart
│   ├── VersionControlRepository.dart
│   ├── FeedbackRepository.dart
│   └── MarketplaceLinksRepository.dart
├── screens/
│   ├── splash_screen/          — Загрузка, проверка версии
│   ├── auth/                   — Авторизация
│   ├── home/                   — Главный экран
│   ├── qr_scan_screen/         — Сканирование для добавления
│   ├── qr_marks_screen/        — Список меток
│   ├── notifications_screen/   — Уведомления + настройки
│   ├── qr_report_event_screen/ — Отправка события
│   ├── status_screen/          — Статус отправки
│   ├── support_screen/
│   └── feedback_screen/
├── widgets/                    — Переиспользуемые компоненты
│   ├── app_bars/
│   ├── buttons/
│   └── text_fields/
└── utils/
    ├── const.dart              — Константы, URL, флаги
    ├── routes.dart             — GetX маршруты
    ├── theme.dart              — Тема
    ├── utils.dart              — Утилиты
    └── i18n/                   — Локализация (русский)
```

### Экраны и маршруты

| Путь | Экран | Контроллер |
|------|-------|-----------|
| `/` | SplashScreen | SplashController |
| `/auth` | AuthScreen | AuthController |
| `/home` | HomeScreen | HomeController |
| `/qr_marks` | QRMarksScreen | QRMarksController |
| `/qr_scan` | QrScanScreen | QRScanController |
| `/notifications` | NotificationsScreen | NotificationsController |
| `/notification_settings` | NotificationSettingsScreen | NotificationSettingsController |
| `/qr_add_event` | QRReportEventScreen | QRReportEventController |
| `/select_event` | SelectEventScreen | — |

### Модели данных

```dart
// Пользователь
class User {
  int? id;
  String phoneNumber;
}

// QR-метка
class QRMark {
  String qrId;          // UUID
  int? seqNumber;
  String? qrName;
  String? status;       // NEW, ACTIVE, DELETED
  DateTime? activateDate;
}

// Уведомление
class CarNotification {
  String id;
  String qrId;
  String qrName;
  int reasonId;
  String? text;
  DateTime? time;
  bool isRead;
}

// Настройки уведомлений
class NotificationSettings {
  bool pushEnabled;
  bool callEnabled;
  bool telegramEnabled;
  bool whatsappEnabled;
  int? telegramDialogId;
}
```

---

## Frontend (Vanilla JS)

### Технологический стек

- HTML5
- CSS3 (без препроцессоров)
- Vanilla JavaScript (ES6 Modules)
- Fetch API

### Структура

```
frontend/
├── index.html          — Главная (маркетинг, ссылки на сторы)
├── notification.html   — Отправка уведомления
├── offer.html          — Публичная оферта
├── privacy-policy.html — Политика конфиденциальности
├── qr.html             — Страница QR
├── css/
│   ├── reset.css       — CSS reset
│   ├── style.css       — Основные стили
│   └── svg-bg.css      — SVG иконки в base64
└── js/
    ├── script.js       — Проверка QR, создание черновика
    ├── sendMsg.js      — Отправка уведомления
    └── systemMessages.js — Шаблоны модальных окон
```

### Страницы

| Страница | Назначение |
|----------|-----------|
| `index.html` | Лендинг: о сервисе, ссылки на сторы, маркетплейсы |
| `notification.html` | Выбор причины и отправка уведомления |
| `offer.html` | Юридический документ: публичная оферта |
| `privacy-policy.html` | Политика конфиденциальности |
| `qr.html` | Точка входа при сканировании QR |

### JavaScript логика

**script.js:**
- `checkQr()` — проверка QR по API
- `createMsg()` — создание черновика уведомления
- Перенаправление на `/notification/{id}`

**sendMsg.js:**
- `buildEventList()` — загрузка причин из API
- `sendMsg(eventId)` — отправка уведомления
- `checkMsgStatus()` — polling статуса (каждые 5 сек)

---

## Потоки данных

### Аутентификация (Mobile)
```
Телефон → LoginAuthMobileController → Zvonok (flashcall)
        ↓
SMS код → LoginAuthMobileController → JWT → GetStorage
```

### Отправка уведомления (Web)
```
QR scan → script.js → /api/web/qr/{id}
        ↓
Создание черновика → /api/report/createDraft
        ↓
Выбор причины → /api/report/updateDraft (status=SEND)
        ↓
Backend → MessageService → [Firebase/Telegram/WhatsApp/SMS/Email/Call]
        ↓
Polling статуса → /api/notification/{id}/status
```

### Получение уведомления (Mobile)
```
Backend → Firebase FCM → PushNotificationController
        ↓
Локальное уведомление → Tap → NotificationsScreen
```
