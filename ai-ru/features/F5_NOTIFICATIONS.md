# F5: Входящие / исходящие уведомления

**Статус:** ✅ В проде · **Последний апдейт:** 2026-04-21

## Что делает

Реестр всех уведомлений в системе — с разделением на **входящие** (пришли владельцу QR) и **исходящие** (отправил сам). Владелец смотрит список в мобильном приложении, помечает прочитанными; факт прочтения возвращается отправителю (если он зарегистрирован) ответным push-уведомлением.

Сама механика создания уведомления — в [F4](../FEATURES.md). Здесь — чтение/пометка/листинг.

## Сценарии

### Входящие у владельца

1. `POST /api/notification.get_all` — пагинированный список по `qr.user_id` (через JOIN qr→notification), плюс `count` и `unreadCount`.
2. `POST /api/notification.get_all_unread` — только `UNREAD`.
3. `POST /api/notification.mark_as_read { notificationId }` → `NotificationFacade.read` → статус `READ`, инкремент метрики, + **ответный push отправителю** если уведомление младше 60 мин (`NOTIFICATION_LIVE_TIME_IN_MIN`).

### Исходящие у пользователя

1. `POST /api/report.get_all` / `report.get_all_unread` — по `sender_id = userId`.
2. `POST /api/report.get { notificationId }` — одно уведомление, доступно только отправителю.

### Проверка статуса (публично)

1. `GET /api/notification/{id}/status` — анонимный endpoint, возвращает только `{ id, status }`. Используется фронтом `qr.html` (после отправки прохожий может видеть «доставлено» / «прочитано»).

### Отметка прочтения из Telegram

1. Inline-callback `/notification/{uuid}` в чате ([F8](../FEATURES.md)) → `NotificationFacade.readBy(dto, userId)` — та же ветка, что и mobile.

### Отметка прочтения из звонка

1. Если уведомление было доставлено через Zvonok (flashcall) — пользователь **перезванивает**, Zvonok-callback приходит в `ZvonokPostBackController` → `notificationService.readByCallId(callId)`. См. [F11](../FEATURES.md).

## Статусы (`NotificationStatus`)

| Статус | Значение |
|--------|----------|
| `DRAFT` | черновик (веб), живёт 5 мин ([F4](../FEATURES.md)) |
| `SEND` | промежуточный из фронта — используется только в `updateDraft` как сигнал «отправляй» |
| `UNREAD` | отправлено владельцу, не прочитано |
| `READ` | прочитано |

## API / Интеграция

| Endpoint | Аутентификация | Описание |
|----------|----------------|----------|
| `POST /api/notification.get_all` | JWT | Входящие (пагинация) |
| `POST /api/notification.get_all_unread` | JWT | Входящие непрочитанные |
| `POST /api/notification.mark_as_read` | JWT | Пометить прочитанным |
| `POST /api/report.get` | JWT | Отправленное уведомление по id |
| `POST /api/report.get_all` | JWT | Исходящие (пагинация) |
| `POST /api/report.get_all_unread` | JWT | Исходящие непрочитанные |
| `GET /api/notification/{id}/status` | нет | Публичный статус |

**Бизнес-правило.** При `mark_as_read` проверяется, что QR принадлежит читающему (`qr.user_id == userId`); иначе `FORBIDDEN`. Повторное чтение — `ALREADY_READ_NOTIFICATION`.

**Ответный push на прочтение** — `NotificationFacade.readBy` считает `LocalDateTime.of(LocalDate.now(), LocalTime.now().minusMinutes(60))` → на переходе через полночь баг (отрицательное время). Та же ошибка, что в `AuthenticationCodeService.isAlreadySent`.

## Реализация

**Backend:**
- `controller/NotificationController` — входящие + пометка.
- `controller/NotificationWebController` — публичный `.../status`.
- `controller/ReportController` — исходящие (`report.get_all`, `report.get_all_unread`, `report.get`).
- `service/NotificationFacade` — оркестрация `send` / `read` / `readBy`.
- `service/NotificationService` — CRUD + пагинация + метрики.
- `repository/NotificationRepository` — JdbcTemplate.

**Mobile:** `lib/screens/notifications_screen` — список входящих/исходящих, swipe-to-read, badge непрочитанных.

**БД (таблица `notification`):** `id uuid`, `qr_id`, `reason_id`, `status`, `sender_id` null-для-web, `visitor_id`, `text`, `created_date`, `call_id` (для Zvonok), `read_date`.

## Ограничения / известный техдолг

- **Bug на переходе через полночь** в `NotificationFacade.readBy` (`LocalTime.now().minusMinutes(60)`). Аналогично `AuthenticationCodeService`.
- **Пагинация без сортировки** явно не указана в `PageParam` — возможны пропуски/дубли на границе страниц если порядок не детерминирован (зависит от реализации репозитория).
- **`report.get_all_unread` и `notification.get_all_unread`** возвращают `NotificationUnreadPage` **без** `unreadCount` (только `count` = непрочитанные). Непоследовательно с `NotificationPage`.
- **Нет фонового уборщика драфтов** (см. [F4](../FEATURES.md)) — `DRAFT` старше 5 мин удаляется только при попытке чтения.
- **Каскадное удаление в `qr.delete`** уничтожает все уведомления юзера по userId, а не только по удаляемому QR (см. [F2](../FEATURES.md) техдолг).
- **Публичный `/notification/{id}/status`** отдаёт enum напрямую — не утечка, но всё же можно скрывать до того, как сам отправитель не авторизовался по `visitorId`.

## Ссылки

- Связанные фичи: [F4](../FEATURES.md) (создание), [F6](../FEATURES.md) (настройки каналов), [F7–F11](../FEATURES.md) (каналы доставки), [F8](../FEATURES.md) (mark_as_read из Telegram), [F11](../FEATURES.md) (mark_as_read из Zvonok).
- Код: `backend/src/main/java/ru/car/controller/Notification*.java`, `ReportController.java`, `backend/src/main/java/ru/car/service/Notification*.java`.
