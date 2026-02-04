# Инструкции для Claude

## Обзор проекта

Car-ID — сервис QR-меток для автомобилей. Позволяет владельцам получать уведомления о событиях с их авто (парковка, ДТП, и т.д.) через push, SMS, звонок, Telegram или WhatsApp.

**Монорепозиторий:**
- `backend/` — Spring Boot API (Java 17)
- `mobile/` — Flutter приложение (iOS/Android)
- `frontend/` — Статический веб-сайт (HTML/CSS/JS)

## Запуск проекта

### Backend (порт 8081)

**Локально (H2 in-memory):**
```bash
cd /Users/kirillreshetov/IdeaProjects/car-id-claude/backend
./gradlew bootRun
```

**С профилем:**
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Mobile (Flutter)

```bash
cd /Users/kirillreshetov/IdeaProjects/car-id-claude/mobile
flutter pub get
flutter run
```

### Frontend

Статический сайт, можно открыть локально или поднять сервер:
```bash
cd /Users/kirillreshetov/IdeaProjects/car-id-claude/frontend
python3 -m http.server 8000
```

## Сборка и тесты

```bash
# Backend
cd backend && ./gradlew build
cd backend && ./gradlew test

# Mobile
cd mobile && flutter test
cd mobile && flutter build apk --release

# Frontend — нет сборки (vanilla JS)
```

## Проверка работоспособности

```bash
curl http://localhost:8081/actuator/health
```

## Конфигурация

### Backend (.env / application.yml)

Файлы конфигурации:
- `backend/src/main/resources/application.yml` — базовый
- `backend/src/main/resources/application-dev.yml` — development (H2)
- `backend/src/main/resources/application-prod.yml` — production (PostgreSQL, encrypted)

**Ключевые переменные (prod зашифрованы через Jasypt):**
- `spring.datasource.*` — PostgreSQL подключение
- `telegram.bot`, `telegram.token` — Telegram бот
- `green-api.*` — WhatsApp интеграция
- `zvonok.*` — SMS/звонки
- `firebase.*` — Push уведомления
- `mail.*` — Email отправка

### Mobile (const.dart)

```dart
const MAIN_URL = 'https://car-id.ru';
const TELEGRAM_BOT_URL = 'https://t.me/car_id_ru_bot';
```

## Документация (ai-ru/)

| Документ | Описание |
|----------|----------|
| [README.md](ai-ru/README.md) | Обзор проекта и содержание |
| [ARCHITECTURE.md](ai-ru/ARCHITECTURE.md) | Архитектура: backend, mobile, frontend |
| [RULES.md](ai-ru/RULES.md) | Бизнес-правила и правила разработки |
| [TECH_REVIEW.md](ai-ru/TECH_REVIEW.md) | Технический обзор: сильные/слабые стороны, план улучшений |
| [PRODUCT_REVIEW.md](ai-ru/PRODUCT_REVIEW.md) | Продуктовый анализ: value prop, монетизация, growth |

## Структура проекта

```
car-id-claude/
├── backend/              # Spring Boot 3.2.3 (Java 17)
│   ├── src/main/java/ru/car/
│   │   ├── controller/   # 13 REST контроллеров
│   │   ├── service/      # Бизнес-логика + интеграции
│   │   ├── repository/   # JdbcTemplate репозитории
│   │   ├── model/        # Entity классы
│   │   └── dto/          # Request/Response DTOs
│   └── src/main/resources/
│       ├── application*.yml
│       └── db/           # Liquibase миграции
├── mobile/               # Flutter (Dart 3.1+)
│   ├── lib/
│   │   ├── controllers/  # GetX контроллеры
│   │   ├── models/       # Модели данных
│   │   ├── repository/   # API клиенты
│   │   ├── screens/      # UI экраны
│   │   └── utils/        # Утилиты, константы
│   └── pubspec.yaml
├── frontend/             # Vanilla HTML/CSS/JS
│   ├── index.html        # Главная страница
│   ├── notification.html # Отправка уведомлений
│   ├── css/
│   └── js/
└── ai-ru/                # Документация на русском
```

## Важно

- **Backend использует JdbcTemplate**, не JPA/Hibernate
- **JWT токены без expiration** — особенность текущей реализации
- **Миграции через Liquibase** — `backend/src/main/resources/db/`
- **Jasypt** для шифрования prod конфигов (пароль: "changeIt" для prod)

## Внешние интеграции

| Сервис | Назначение | Конфиг |
|--------|-----------|--------|
| **Zvonok** | Flashcall + голосовые звонки | `zvonok.*` |
| **SMS Aero** | SMS сообщения | в коде |
| **Firebase** | Push уведомления | `firebase.*` |
| **Telegram** | Telegram бот | `telegram.*` |
| **WhatsApp** | Green API | `green-api.*` |
| **Email** | Jakarta Mail | `mail.*` |

## API Endpoints (основные)

### Аутентификация
```
POST /api/user.login_oauth_mobile  — Запросить код (flashcall)
POST /api/user.login_oauth_code    — Подтвердить код → JWT
POST /api/user.logout              — Выход
```

### QR коды
```
POST /api/qr.get_all      — Все QR пользователя
POST /api/qr.create       — Создать QR
POST /api/qr.link_to_user — Привязать QR к аккаунту
POST /api/qr.delete       — Удалить QR
```

### Уведомления
```
POST /api/notification.get_all        — Входящие уведомления
POST /api/notification.mark_as_read   — Отметить прочитанным
POST /api/report.send                 — Отправить уведомление на QR
POST /api/report.get_all_reasons      — Справочник причин
```

## Статусы сущностей

### QR Status
- `NEW` — создан, не активирован
- `ACTIVE` — привязан к пользователю
- `TEMPORARY` — временный (удаляется через 2 часа)
- `DELETED` — удалён

### Notification Status
- `DRAFT` — черновик (удаляется через 5 минут)
- `SEND` — отправлено
- `UNREAD` — доставлено, не прочитано
- `READ` — прочитано

## Мониторинг

```bash
# Health
curl http://localhost:8081/actuator/health

# Prometheus метрики
curl http://localhost:8081/actuator/prometheus
```

## Продакшн

- **Домен:** car-id.ru
- **Backend:** Spring Boot за nginx
- **База:** PostgreSQL (encrypted connection string)
- **Профиль:** `prod` с Jasypt шифрованием
