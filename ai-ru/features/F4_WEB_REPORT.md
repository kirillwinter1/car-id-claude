# F4: Веб-отправка уведомления при сканировании

**Статус:** ✅ В проде · **Последний апдейт:** 2026-04-21

## Что делает

Анонимный «прохожий» сканирует QR на автомобиле → открывается `car-id.ru/qr/{uuid}` → выбирает событие из списка → отправляется уведомление владельцу. Без регистрации, без авторизации. Используется как основной путь взаимодействия случайных людей с сервисом.

## Сценарии

### Отправка через фронт

1. Прохожий сканирует QR камерой телефона → открывается `/qr/{uuid}` (фронт).
2. `script.js`: из URL достаёт UUID, вызывает `GET /api/qr/{id}` ([F2](../FEATURES.md)).
3. Если статус `ACTIVE` или `TEMPORARY` — форма показывается; иначе ошибка.
4. Параллельно фронт вызывает `GET /api/report/get_all_reasons` — список причин ([F13](../FEATURES.md)).
5. Прохожий выбирает причину → `POST /api/report/createDraft { qrId, reasonId, visitorId }` — создаётся `Notification` в статусе `DRAFT`. Возвращается `notificationId`.
6. После подтверждения → `PUT /api/report/updateDraft { notificationId, status: SEND, reasonId, text? }` — статус меняется на `UNREAD`, запускается рассылка по всем каналам владельца (`MessageService.sendPush`).
7. Фронт показывает «сообщение доставлено»; при прочтении владельцем — опционально показывает статус (polling `report/get` не используется в текущем коде).

### Отправка через мобильное приложение (от зарегистрированного пользователя)

1. Авторизованный пользователь: `POST /api/report.send { qrId, reasonId, text }` → `NotificationFacade.send` → сразу создаёт `UNREAD` и шлёт push. Без стадии `DRAFT`.

## Статусы (`NotificationStatus`) в рамках F4

| Статус | Описание |
|--------|----------|
| `DRAFT` | черновик; автоудаление через 5 минут (`NOTIFICATION_LIVE_TIME_IN_MIN`). См. `NotificationService.findByIdOrThrowNotFound`. |
| `SEND` | промежуточный — в `updateDraft` фронт присылает его в `status`, код меняет на `UNREAD`. |
| `UNREAD` | отправлено, владелец ещё не прочёл. |
| `READ` | владелец отметил прочитанным. |

## API / Интеграция

| Endpoint | Аутентификация | Описание |
|----------|----------------|----------|
| `GET /api/report/get_all_reasons` | нет | Список причин (web) |
| `POST /api/report/createDraft` | нет | Создать черновик |
| `PUT /api/report/updateDraft` | нет | Обновить/отправить черновик |
| `POST /api/report/send` | нет | Прямая отправка без черновика (дубль?) |
| `POST /api/report.send` | JWT | Отправка из mobile |

**Rate-limit:** при `updateDraft` → `SEND` ищется уведомление на этот QR за последнюю минуту (`findByQrIdAndDateAfter`), если есть — `SEND_TIMEOUT` ошибка. Фронт в `notification.html` заявляет «1 раз в час» — это расхождение текста и реализации.

**Visitor ID:** фронт отдаёт `visitorId` (fingerprint), backend сохраняет в `notification.visitor_id`. Валидация `isValid` написана, но **не вызывается** — см. техдолг.

## Реализация

**Backend:**
- `controller/ReportWebController` — анонимные endpoints.
- `controller/ReportController` — mobile endpoint + получение отправленных.
- `service/NotificationFacade` — оркестрация `send` / `read` / `updateAndTrySendDraft`.
- `service/NotificationService` — CRUD + статусы + rate-limit.
- `service/message/MessageService.sendPush` — веер каналов ([F5](../FEATURES.md)).

**Frontend:**
- `frontend/qr.html` — лэндинг при сканировании; подключает `script.js`.
- `frontend/notification.html` — форма выбора события; `sendMsg.js` делает `updateDraft`.
- `frontend/js/script.js` — проверка QR, создание draft.
- `frontend/js/sendMsg.js` — построение списка причин, отправка.
- `frontend/js/systemMessages.js` — тексты «ожидание», «прочитано».

**БД (таблица `notification`):** `id`, `qr_id`, `reason_id`, `status`, `sender_id` (null для веб), `visitor_id`, `text`, `created_date`, `call_id`.

## Ограничения / известный техдолг

- **Rate-limit 1 мин в коде vs 1 час в тексте фронта** — расхождение, нужно согласовать.
- **`isValid(visitorId)`** определён в `NotificationService`, но не используется — любая строка пишется в БД.
- **`/api/report/send` (web, без черновика)** — дубликат пути с `createDraft → updateDraft → SEND`; не используется фронтом, но висит. Кандидат на удаление.
- **Автоудаление черновиков** работает «лениво»: `findById` удаляет драфт старше 5 мин при запросе к нему. Если запроса нет — черновик висит в БД. Нет фонового уборщика.
- **Нет rate-limit по IP/visitorId** — ограничение только «одно уведомление на QR в минуту»; можно завалить QR кучей visitor-id.
- **Анонимные endpoints не документированы отдельно** — в коде смешаны с защищёнными контроллерами.
- **`NotificationFacade.readBy` логика «напомнить senderу»** — если уведомление создано веб-анонимом (`senderId == null`), `sendReadPush` тихо выходит; OK, но неявно.

## Ссылки

- Связанные фичи: [F2](../FEATURES.md) (QR публичный GET), [F5](../FEATURES.md) (получение уведомлений владельцем), [F13](../FEATURES.md) (справочник причин), [F7–F11](../FEATURES.md) (каналы доставки).
- Код: `backend/src/main/java/ru/car/controller/Report*.java`, `backend/src/main/java/ru/car/service/Notification*.java`, `frontend/qr.html`, `frontend/notification.html`, `frontend/js/`.
