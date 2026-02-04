# Правила Car-ID

> Бизнес-правила + правила разработки

## Часть 1: Бизнес-правила

### Ключевые принципы

1. **Мультиканальность уведомлений.** Пользователь сам выбирает способы: push, звонок, Telegram, WhatsApp, SMS.
2. **Анонимность отправителя.** При отправке через веб — только `visitorId` (без регистрации).
3. **QR-метка — центральная сущность.** Все уведомления привязаны к QR.

### Статусы QR-меток

| Статус | Описание |
|--------|----------|
| `NEW` | Создан, но не привязан к пользователю |
| `ACTIVE` | Привязан к аккаунту, работает |
| `TEMPORARY` | Временный (удаляется через 2 часа scheduler'ом) |
| `DELETED` | Удалён (soft delete) |

### Статусы уведомлений

| Статус | Описание |
|--------|----------|
| `DRAFT` | Черновик (автоудаление через 5 минут) |
| `SEND` | Отправлено, ожидает доставки |
| `UNREAD` | Доставлено, не прочитано |
| `READ` | Прочитано владельцем |

### Аутентификация

- **Flashcall** — основной способ (Zvonok)
- **SMS** — backup через SMS Aero
- **Код** — 4 цифры, верифицируется на backend
- **JWT** — токен без expiration (особенность текущей реализации)

### Способы уведомления (приоритет)

1. **Push** (Firebase FCM) — основной
2. **Telegram Bot** — если включен
3. **WhatsApp** (Green API) — если включен
4. **Звонок** (Zvonok) — если включен
5. **SMS** — fallback
6. **Email** — для feedback

### Ограничения

- **Web:** 1 уведомление в час (на уровне UI)
- **Mobile:** без ограничений (авторизованный пользователь)
- **Временные QR:** автоудаление каждые 2 часа
- **Черновики:** автоудаление через 5 минут

### Справочник причин (ReasonDictionary)

Загружается из API `/api/report.get_all_reasons`. Примеры:
- Припаркован на месте инвалида
- Мешает проезду
- Повреждение автомобиля
- Оставлены включёнными фары
- Другое

---

## Часть 2: Правила разработки

### Backend

#### Data Access
- **JdbcTemplate** — не JPA/Hibernate
- Все запросы в Repository классах
- `RowMapper` для маппинга результатов

#### Security
- **JWT** без expiration (исторически)
- **CORS** открыт для всех (`*`)
- **Jasypt** для шифрования prod конфигов
- Whitelist для публичных endpoints (login, feedback, version, web API)

#### Миграции
- **Liquibase** — `backend/src/main/resources/db/`
- `master.xml` — точка входа
- `changelog/` — версионные миграции

#### Асинхронность
- **ExecutorService** (8 потоков) для отправки уведомлений
- **CompletableFuture** для параллельной отправки по каналам
- **@Scheduled** для cleanup задач

#### API стиль
- Все endpoints через POST (кроме Web API)
- Request/Response через DTO
- Централизованная обработка ошибок через `RestExceptionHandler`

### Mobile (Flutter)

#### State Management
- **GetX** — единственный state management
- **GetConnect** — HTTP клиент в репозиториях
- **GetStorage** — локальное хранилище

#### Архитектура
```
Screen → Controller → Repository → API
           ↓
        Model (данные)
```

#### Константы
- Все URL и ключи в `lib/utils/const.dart`
- Не хардкодить в коде компонентов

#### Локализация
- Только русский язык (`ru_RU`)
- Строки в `lib/utils/i18n/lang/ru_RU.dart`

### Frontend

#### Принципы
- **Vanilla JS** — без фреймворков
- **ES6 Modules** — `type="module"` в script тегах
- **Fetch API** — для HTTP запросов

#### API взаимодействие
- Все через `/api/` endpoints
- Polling для статуса (каждые 5 сек)
- Модальные окна для UX feedback

---

## Часть 3: Чеклист для задач

### Перед коммитом

- [ ] Тесты проходят (`./gradlew test`)
- [ ] Приложение собирается (`./gradlew build`)
- [ ] Нет хардкода sensitive данных
- [ ] Миграции backward-compatible

### При добавлении API endpoint

- [ ] DTO для request/response
- [ ] Обработка ошибок (NotFoundException, etc.)
- [ ] Документация в коде
- [ ] Обновить ARCHITECTURE.md если нужно

### При добавлении интеграции

- [ ] Конфиг через application.yml
- [ ] Зашифровать sensitive данные (Jasypt) для prod
- [ ] Fallback при недоступности сервиса
- [ ] Логирование ошибок

---

## Часть 4: Известные особенности

### Backend

1. **JWT без expiration** — токены живут вечно
2. **CORS открыт для всех** — `*` в конфиге
3. **H2 для dev** — in-memory, данные не сохраняются
4. **Admin через конфиг** — hardcoded phone/code в application.yml

### Mobile

1. **Два типа сборки Android** — Google Play и Huawei AppGallery
2. **Firebase для push** — требует GoogleServices
3. **QR формат** — только `https://car-id.ru/qr/{UUID}`

### Frontend

1. **Статический сайт** — нет сборки
2. **Polling статуса** — каждые 5 секунд
3. **Ограничение 1 раз в час** — только UI (не backend)

---

## Часть 5: Внешние сервисы

| Сервис | Назначение | Документация |
|--------|-----------|--------------|
| **Zvonok** | Flashcall + голосовые звонки | zvonok.com |
| **SMS Aero** | SMS отправка | smsaero.ru |
| **Firebase** | Push уведомления | firebase.google.com |
| **Telegram Bot API** | Telegram интеграция | core.telegram.org/bots |
| **Green API** | WhatsApp интеграция | green-api.com |
| **Google ZXing** | Генерация QR-кодов | github.com/zxing/zxing |
