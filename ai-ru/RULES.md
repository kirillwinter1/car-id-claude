# Правила Car-ID

> Бизнес-правила + правила разработки. Статусы сущностей и детали фич — в [features/](features/), техдолг — в [TECH_DEBT.md](TECH_DEBT.md).

## Часть 1 — Бизнес-правила

### Ключевые принципы

1. **Мультиканальная доставка уведомлений.** Пользователь выбирает: push, звонок, Telegram. SMS — только как резерв для кода авторизации. Email как канал уведомлений не используется.
2. **Анонимность отправителя с веба.** При сканировании QR прохожим регистрация не требуется — идентификатор `visitor_id` (хэш/fingerprint).
3. **QR-метка — центральная сущность.** Все уведомления привязаны к конкретному QR (и через него — к его владельцу).
4. **Soft-delete** для QR (`status = DELETED`) и пользователя (`active = false`). Исключение: временный QR удаляется жёстко планировщиком (см. [TECH_DEBT B10](TECH_DEBT.md)).

### Аутентификация

- **Flashcall** (Zvonok) — основной способ. Последние 4 цифры caller-id = код.
- **SMS** (SMS Aero) — **резерв** для кода: шлётся через 30 сек, если код не введён.
- **Голосовой звонок** (`Zvonok.sendCodeMessage`) — альтернативный резерв.
- **JWT** — токен без `exp` claim (см. [TECH_DEBT S1](TECH_DEBT.md); исправить = важнейший приоритет).
- **Admin-логин** — через `admin.phone` + `admin.code` из конфига (обходит звонок). См. [TECH_DEBT S3](TECH_DEBT.md).

### Ограничения, зашитые в коде

| Что | Где в коде | Значение |
|-----|------------|----------|
| Rate-limit на повторный код по одному телефону | `AuthenticationCodeService.isAlreadySent` + `ApplicationConstants.SMS_NEXT_REQUEST_TIMEOUT_IN_SEC` | таймаут в секундах (см. константу; баг на границе суток — [TECH_DEBT B1](TECH_DEBT.md)) |
| «Ответный push о прочтении» приходит только если оригинал младше | `NotificationFacade.readBy` + `ApplicationConstants.NOTIFICATION_LIVE_TIME_IN_MIN` | 60 минут |
| Автоудаление DRAFT-уведомления | `NotificationService.findByIdOrThrowNotFound` | 5 минут, **лениво** (только при чтении) |
| Rate-limit повторной веб-отправки на один QR | `NotificationService.updateDraft` | **1 минута** (код); на `notification.html` фронт показывает «1 раз в час» — см. [TECH_DEBT B4](TECH_DEBT.md) |
| Temporary QR живёт | `QrService.destroyAllTemporaryQr` + `TemporaryQrScheduler(2h)` | TTL `[1 ч, 3 ч]` фактически; **документация говорит «2 часа»** — см. [TECH_DEBT B3](TECH_DEBT.md) |

> Значения намеренно берутся из кода — если нужно изменить поведение, правь код и эту таблицу **вместе**.

### Справочник причин

- Загружается из `reason_dictionary` через `GET /api/report/get_all_reasons` (web) / `POST /api/report.get_all_reasons` (mobile).
- Текущий seed (из `changelog-1.1.xml`): «Автомобиль мешает», «Автомобиль эвакуируют», «Произошло ДТП», «Открыто окно», «Работает сигнализация», «Спущено колесо», «Мойка завершена», «Ремонт завершен».
- Добавление — только миграцией Liquibase.

---

## Часть 2 — Правила разработки

### Backend

- **Data access: `NamedParameterJdbcTemplate`.** Не JPA/Hibernate. Все запросы — в `repository/*.java`; `BeanPropertyRowMapper` для маппинга.
- **Маппинг DTO/Entity — MapStruct** (`mapper/`).
- **Миграции — Liquibase.** Единая точка входа `db/master.xml`, changelog-файлы `db/changelog/changelog-N.M.xml`. Новые миграции — всегда новый файл с инкрементальным номером.
- **Конфиги prod зашифрованы Jasypt** (`ENC(...)` в `application-prod.yml`; пароль в env/CLI).
- **Централизованная обработка ошибок** — `exception/RestExceptionHandler`. Для бизнес-ошибок — `BadRequestException`/`ForbiddenException`/`NotFoundException`/`UnauthorizedException` + `ErrorCode` enum.
- **Асинхронность** — `ExecutorService pushExecutorService` (8 потоков), `CompletableFuture.delayedExecutor(..)` для отложенного запуска.
- **API-стиль** — все endpoint'ы через `POST` (кроме web-путей в стиле REST). Ответ mobile обёрнут в `MobileRs<T>`; web — plain JSON. См. [TECH_DEBT A6](TECH_DEBT.md).
- **Логирование** — slf4j + Lombok `@Slf4j`. Не логировать коды авторизации и полные токены (см. [TECH_DEBT S9](TECH_DEBT.md)).

### Mobile (Flutter)

- **State management — только GetX.** DI через `Get.put`/`Get.find`, routing через `Get.toNamed`, state через `GetxController` + `GetBuilder`.
- **API-слой — `repository/*.dart`** на `GetConnect`. Контроллер никогда не дёргает сеть напрямую.
- **Константы и URL** — только в `lib/utils/const.dart`. Никаких хардкодов в компонентах.
- **i18n** — файл `lib/utils/i18n/lang/ru_RU.dart`. Пока только русский.

### Frontend

- **Без фреймворка.** Vanilla JS, ES6 modules (`type="module"`).
- **Fetch API** для HTTP. Текст ошибок — через `systemMessages.js`.
- **Polling статуса** — раз в 5 сек. Кандидат в WebSocket/SSE (см. [TECH_DEBT W1](TECH_DEBT.md)).

### Checkhouse перед коммитом

- [ ] `./gradlew build` в `backend/` — чисто.
- [ ] `./gradlew test` — без падений (если тесты есть для затронутой области).
- [ ] `flutter analyze` в `mobile/` — без ошибок.
- [ ] Новые секреты не попали в репо (проверь diff по `.yml` / `.json` / `.dart`).
- [ ] Новая миграция Liquibase добавлена в `master.xml`.
- [ ] Изменения отражены в соответствующей карточке [features/](features/) или [TECH_DEBT.md](TECH_DEBT.md).

### При добавлении API endpoint

- Request/Response через отдельные DTO в `dto/mobile/` или `dto/web/`.
- Swagger-аннотации (`@Operation`, `@ApiResponses`) — обязательны.
- В бизнес-ошибках — использовать `ErrorCode`.
- Для публичных (без JWT) endpoint'ов — проверить whitelist в `SecurityConfiguration`.

### При добавлении интеграции

- Конфигурация — через `@ConfigurationProperties(...)` в `application*.yml`.
- Prod-секреты — через Jasypt `ENC(...)`.
- В `MessageService` добавлять через интерфейс `Sender`.
- Добавить fallback или retry для сетевых ошибок.
- Логировать success/failure, но без sensitive-данных.

---

## Часть 3 — Внешние сервисы

| Сервис | Назначение | Конфиг |
|--------|-----------|--------|
| **Zvonok** | Flashcall + голосовые звонки | `zvonok.*` |
| **SMS Aero** | SMS (резерв для auth) | захардкожен в `SmsService` — см. [TECH_DEBT S5](TECH_DEBT.md) |
| **Firebase FCM** | Push-уведомления | `firebase.*` |
| **Telegram Bot API** | Telegram-бот | `telegram.*` |
| **Google ZXing** | Генерация QR в SVG | `com.google.zxing:*` |

## Часть 4 — Известные особенности

- **JWT без expiration** — историческая особенность; токены живут вечно. См. [TECH_DEBT S1](TECH_DEBT.md).
- **CORS открыт для всех** (`*`) — см. [TECH_DEBT S2](TECH_DEBT.md).
- **H2 in-memory для dev** — данные не сохраняются между запусками.
- **QR формат** — только `https://car-id.ru/qr/{UUID}`. Mobile валидирует этот формат.
- **Два типа сборки Android** — Google Play и RuStore (Huawei AppGallery сейчас не поддерживается из-за зависимости от FCM).

## Ссылки

- [ARCHITECTURE.md](ARCHITECTURE.md) — карта кода.
- [FEATURES.md](FEATURES.md) — список фич.
- [TECH_DEBT.md](TECH_DEBT.md) — технический долг.
- [OPERATIONS.md](OPERATIONS.md) — операционный runbook.
