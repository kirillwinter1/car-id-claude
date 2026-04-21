# F2: QR-коды (создание, привязка, типы)

**Статус:** ✅ В проде · **Последний апдейт:** 2026-04-21

## Что делает

Жизненный цикл QR-наклейки, которая клеится на автомобиль. QR создаётся в системе (батчи для печати), затем привязывается пользователем к аккаунту через сканирование. По UUID в QR-коде любой прохожий может открыть веб-страницу и отправить владельцу уведомление (см. [F4](../FEATURES.md)).

## Сценарии

### Массовая печать (админская)

1. Админ вызывает `POST /api/qr.create` — создаётся `Qr` в статусе `NEW`, привязанный к `batch_id`.
2. QR печатается с UUID в ссылке: `https://car-id.ru/qr/{uuid}`.

### Привязка QR пользователем

1. Mobile: пользователь сканирует QR с наклейки → получает UUID.
2. `POST /api/qr.link_to_user { qrId, qrName }` — находит QR, требует статус `NEW`, ставит `user_id`, имя, статус `ACTIVE`.
3. Если QR уже `ACTIVE` — ошибка `ALREADY_ACTIVE_QR`. Если `TEMPORARY` — ошибка `TEMPORARY_QR` (временные нельзя привязать).

### Получение списка

1. `POST /api/qr.get_all` → все QR текущего пользователя.
2. `POST /api/qr.get { qrId }` → один QR по id.

### Удаление

1. `POST /api/qr.delete { qrId }` — статус → `DELETED`, каскадно удаляются все уведомления пользователя (`notificationRepository.deleteAllByUserId` — см. техдолг).
2. Временные QR удалять нельзя — `ForbiddenException`.

### Веб-доступ (анонимный)

1. `GET /api/qr/{id}` → публичные данные QR (минимальный DTO без владельца). Используется фронтом при сканировании незнакомцем.

## Статусы (`QrStatus`)

| Статус | Описание |
|--------|----------|
| `NEW` | создан, не привязан |
| `ACTIVE` | привязан к пользователю |
| `TEMPORARY` | временный, TTL ≈ 2 часа ([F3](../FEATURES.md)) |
| `DELETED` | soft-delete |

## API / Интеграция

| Endpoint | Описание |
|----------|----------|
| `POST /api/qr.create` | Создать новый QR (статус `NEW`) |
| `POST /api/qr.create_temporary` | Создать временный QR (см. [F3](../FEATURES.md)) |
| `POST /api/qr.link_to_user` | Привязать QR к текущему пользователю |
| `POST /api/qr.get` | Данные QR по id |
| `POST /api/qr.get_all` | Все QR пользователя |
| `POST /api/qr.delete` | Soft-delete QR |
| `GET /api/qr/{id}` | Публичные данные QR (анонимный доступ) |

## Реализация

**Backend:**
- `controller/QrController` — mobile endpoints (`qr.*`), требует JWT.
- `controller/QrWebController` — один публичный GET для веб-страницы.
- `service/QrService` — вся бизнес-логика: create / link / delete / findActive.
- `repository/QrRepository` — JdbcTemplate.
- `mapper/QrDtoMapper`, `mapper/QrWebDtoMapper` — в DTO (web — урезанный).
- `scheduler/TemporaryQrScheduler` — `@Scheduled(fixedDelay = 2h)` вызывает `destroyAllTemporaryQr`.

**Mobile:** `lib/screens/qr_marks_screen` (список), `lib/screens/qr_scan_screen` (сканирование → `qr.link_to_user`).

**БД (таблица `qr`):**
- `id uuid` — публичный идентификатор на наклейке.
- `user_id` — владелец (null до привязки).
- `batch_id` — к какому батчу печати относится.
- `status`, `name`, `seq_number`, `printed`, `created_at`.

**Связанные сущности:**
- `Batch` — партия для печати (шаблон QR: `BatchTemplates`).
- QR печатается как PDF (`SvgUtils.getSvgText` + `PdfConverter.create`, вызов из Telegram-бота в [F8](../FEATURES.md)).

## Ограничения / известный техдолг

- **Жёсткий каскад при `qr.delete`:** `notificationRepository.deleteAllByUserId(userId)` удаляет **ВСЕ** уведомления пользователя, а не только по этому QR. Баг.
- **`destroyAllTemporaryQr` удаляет старше 1 часа** (`LocalDateTime.now().minusHours(1L)`), а документация и код `QrStatus.TEMPORARY` описывают 2 часа. Несогласованность; фактический TTL = `[1h, 3h]` из-за интервала в 2 часа планировщика.
- **`createTemporaryQr`** кидает `NotFoundException` с кодом `ALREADY_HAS_QR`, если у пользователя уже **хотя бы один** QR — но проверка идёт без фильтра по статусу, т.е. удалённые тоже блокируют создание временного.
- **`findActiveQrById`** проверяет существование отдельным запросом + `findByIdOrThrowNotFound` — два SELECT там, где хватило бы одного.
- `batchId` всегда захардкожен `= 1L` в `createQr` и `createTemporaryQr` — батч как сущность есть, но механика его назначения не используется.
- `printed` флаг не используется в коде (только хранится).
- Нет permission-check на `qr.create` — любой авторизованный пользователь может создать новый QR (предполагается админ-онли).

## Ссылки

- Связанные фичи: [F3](../FEATURES.md) (временные QR), [F4](../FEATURES.md) (веб-отправка уведомления), [F5](../FEATURES.md) (уведомления), [F8](../FEATURES.md) (выгрузка PDF через Telegram).
- Код: `backend/src/main/java/ru/car/service/QrService.java`, `backend/src/main/java/ru/car/controller/Qr*.java`, `backend/src/main/java/ru/car/scheduler/TemporaryQrScheduler.java`.
