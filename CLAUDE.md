# Инструкции для Claude

## Обзор проекта

Car-ID — сервис QR-меток для автомобилей. Владелец получает уведомления о событиях с его авто (парковка, ДТП и т.д.) через push, звонок или Telegram. Веб-отправка уведомления анонимна (без регистрации).

**Монорепозиторий:**
- `backend/` — Spring Boot API (Java 17)
- `mobile/` — Flutter (iOS/Android)
- `frontend/` — статический сайт (HTML/CSS/JS, vanilla)

## Документация

**Вся смысловая документация — в [`ai-ru/`](ai-ru/README.md).** Дубли в других местах не плодим.

Минимум, с которого стоит начать:

| Документ | О чём |
|----------|-------|
| [ai-ru/README.md](ai-ru/README.md) | Обзор и быстрый старт |
| [ai-ru/FEATURES.md](ai-ru/FEATURES.md) | Индекс всех фич + ссылки на карточки |
| [ai-ru/ARCHITECTURE.md](ai-ru/ARCHITECTURE.md) | Карта кода |
| [ai-ru/RULES.md](ai-ru/RULES.md) | Бизнес-правила + правила разработки |
| [ai-ru/TECH_DEBT.md](ai-ru/TECH_DEBT.md) | Единый реестр техдолга (security, bugs, dead code, architecture, tests, devops) |
| [ai-ru/OPERATIONS.md](ai-ru/OPERATIONS.md) | Runbook (структура; чувствительная часть — в приватном `ops.inf` вне git) |
| [ai-ru/PRODUCT_REVIEW.md](ai-ru/PRODUCT_REVIEW.md) | Продуктовый анализ |

## Запуск

### Backend (порт 8081)

```bash
cd backend
./gradlew bootRun                                         # H2 in-memory
./gradlew bootRun --args='--spring.profiles.active=dev'   # dev-профиль
```

### Mobile

```bash
cd mobile
flutter pub get
flutter run
```

### Frontend

```bash
cd frontend && python3 -m http.server 8000
```

## Сборка и тесты

```bash
cd backend && ./gradlew build
cd backend && ./gradlew test
cd mobile  && flutter test
cd mobile  && flutter build apk --release
# Frontend — сборки нет (vanilla JS)
```

## Health

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8081/actuator/prometheus
```

## Конфигурация

- `backend/src/main/resources/application.yml` — базовый.
- `backend/src/main/resources/application-dev.yml` — dev (H2, тестовые значения).
- `backend/src/main/resources/application-prod.yml` — prod (PostgreSQL + Jasypt-шифрование `ENC(...)`).

**Важно:**
- Jasypt-пароль для prod: `changeIt` (задаётся через `JASYPT_ENCRYPTOR_PASSWORD` env или `-Djasypt.encryptor.password`).
- Секреты SMS Aero сейчас **захардкожены в коде** (`SmsService.java`) — см. [TECH_DEBT S5](ai-ru/TECH_DEBT.md).

**Ключевые properties-блоки:**
- `spring.datasource.*` — PostgreSQL.
- `telegram.*` — Telegram-бот и feedback-канал.
- `firebase.*` — FCM service account.
- `zvonok.*` — flashcall + voice.

**Mobile (`lib/utils/const.dart`):**
```dart
const MAIN_URL = 'https://car-id.ru';
const TELEGRAM_BOT_URL = 'https://t.me/car_id_ru_bot';
```

## Текущая фаза работ

**Стабилизация.** Новые фичи сейчас не добавляем. Порядок этапов:

1. Документация (в работе, см. `ai-ru/`).
2. Точечные фиксы кода (мёртвый код, очевидные баги).
3. Тесты на ядро.
4. Код-ревью.
5. Рефакторинг (начиная с Telegram — см. [TECH_DEBT A1–A4](ai-ru/TECH_DEBT.md)).
6. **Только после этого** — новые фичи.

Если обнаруживаешь идею новой функциональности — заводи BF-карточку в [`ai-ru/backlog/`](ai-ru/backlog/), не реализуй.

## Важные особенности

- **Backend на `JdbcTemplate`**, не JPA/Hibernate.
- **JWT без expiration** — вечные токены (см. [TECH_DEBT S1](ai-ru/TECH_DEBT.md)).
- **Миграции Liquibase** — новый файл `changelog-N.M.xml` + строка в `master.xml`.
- **Web-отправка уведомления анонимна** (по `visitor_id`).
- **Temporary QR TTL** — фактически `[1h, 3h]` (планировщик раз в 2 часа, порог «старше 1 часа»); см. [TECH_DEBT B3](ai-ru/TECH_DEBT.md).

## Продакшн

- **Домен:** `car-id.ru`.
- **Backend:** Spring Boot за nginx, systemd-сервис.
- **БД:** PostgreSQL (encrypted connection в prod-конфиге).
- Операционные детали (IP сервера, команды деплоя, SSL-процедура) — в приватном `ops.inf` (вне git, только на машине владельца).
