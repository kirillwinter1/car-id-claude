# F8: Telegram-бот (уведомления + команды)

**Статус:** ✅ В проде · **Последний апдейт:** 2026-04-21

## Что делает

Telegram-бот решает две задачи:

1. **Канал доставки уведомлений** — если пользователь привязал Telegram в настройках ([F6](../FEATURES.md)), входящие события по его QR-кодам приходят ему в личку с кнопкой «отметить прочитанным».
2. **Интерактивный помощник** — даёт пользователю посмотреть свои QR-коды, выгрузить PDF-файл c QR, создать временный QR без захода в приложение. Для админов — быстрая суточная статистика.

В продакшене — `@car_id_ru_bot`, в dev — `@car_id_test_bot`. Запуск — long polling (не webhook).

## Сценарии

### Привязка Telegram к аккаунту

1. Пользователь пишет боту → бот видит, что `telegram_dialog_id` незнаком.
2. Бот запрашивает контакт (кнопка «поделиться контактом»).
3. Если телефон есть в таблице `users` — записывается `notification_setting.telegram_dialog_id = chatId`, выдаётся приветствие + меню.
4. Если телефона нет — бот предлагает скачать мобильное приложение.

### Получение уведомления

1. Backend отправил `sendPush` → `TelegramBotService.sendNotification(TextMessage)` через `MessageService.asyncSend`.
2. Сообщение приходит в чат с inline-кнопкой «отметить прочитанным» (callback = `/notification/{uuid}`).
3. При клике: `TelegramLogicService` вызывает `NotificationFacade.readBy()` и убирает кнопку.

### QR-коды через бота

1. Пользователь жмёт кнопку «QR-коды» → бот отвечает списком inline-кнопок (имя + серийный номер).
2. Клик по QR → асинхронная генерация PDF (`PdfConverter.create(SvgUtils.getSvgText)`) → отправка документом `qr.pdf`.

### Временный QR

1. Кнопка «Временный QR» → `QrService.createTemporaryQr(userId)` → возвращает ссылку `{url}/qr/{id}` с TTL 2 часа (см. [F3](../FEATURES.md)).

### Админ-меню

Пользователи с `user.is_admin = true` видят дополнительные кнопки: `/registerUserCount`, `/activateQrCount`, `/sendNotificationCount`, `/readNotificationCount` — данные из `MonitoringService` за сегодня.

## API / Интеграция

**Telegram Bot API** через `org.telegram.telegrambots` (long polling, `DefaultBotSession`).

**Команды-кнопки** (`TelegramMenu.*_CMD`):

| Константа | Значение | Кто обрабатывает |
|-----------|----------|------------------|
| `QRS_CMD` | "QR-коды" | user / admin menu |
| `TEMPORARY_QR_CMD` | "Временный QR" | user / admin menu |
| `QR_CMD` | "/qr/" + UUID | inline callback → PDF |
| `NOTIFICATION_CMD` | "/notification/" + UUID | inline callback → mark read |
| `REGISTER_USER_CMD` / `ACTIVATE_QR_CMD` / `SEND_NOTIFICATION_CMD` / `READ_NOTIFICATION_CMD` | `/...Count` | admin menu |
| `CONTACT_CMD` | "поделиться контактом" | шаг авторизации |

**Реализация интерфейса `Sender`** — бот участвует в веере каналов доставки через `MessageService.asyncSend` наряду с Firebase и Zvonok.

**Feedback-канал:** `TelegramBotService.sendFeedback(from, text)` — пересылает обратную связь из мобильного приложения в канал `telegram.feedbackChannelId` (см. [F12](../FEATURES.md)).

## Реализация

**Backend (пакет `ru.car.service.message.telegram`):**
- `TelegramBotService` — наследует `TelegramLongPollingBot`, реализует `Sender`. Низкоуровневая отправка + бизнес-методы `sendQrs`, `sendQr` (PDF), `sendFeedback`, `editMarkup`.
- `TelegramLogicService` — маршрутизация `onUpdateReceived`: авторизация → callback-хендлеры → switch по текстовым командам.
- `TelegramMenu` — статические фабрики `ReplyKeyboardMarkup` (user / admin / contact) + inline-клавиатуры + константы команд.
- `TelegramConfig` — `@PostConstruct` bootstrap: разрывает циклическую зависимость через setter-инъекцию `TelegramLogicService` в `TelegramBotService`, затем `bot.init()`.
- `TelegramProperties` — `@ConfigurationProperties("telegram")`: `bot`, `token` (Jasypt), `feedbackChannelId`, `enable`.

**БД (таблица `notification_setting`):**
- `telegram_enabled boolean not null default false` — канал включён.
- `telegram_dialog_id bigint null` — chatId пользователя; по нему бот определяет, кто пишет.

**Конфигурация (`application*.yml`):**
- `telegram.bot`, `telegram.token`, `telegram.feedbackChannelId`, `telegram.enable`.
- В prod — токен зашифрован Jasypt.

## Ограничения / известный техдолг

- **Циклическая зависимость** `TelegramBotService ↔ TelegramLogicService` разрешается через `@Setter` и ручной bootstrap в `TelegramConfig` — хрупкая конструкция.
- **`@Setter` на уровне класса** `TelegramBotService` делает мутируемыми все поля, включая репозитории и меню.
- **Монолитный `onUpdateReceived`** в `TelegramLogicService` — авторизация, callback-логика и switch по командам в одной функции; новая команда = +case в switch, сложно тестировать.
- **`catch (Exception ignore) {}`** при пометке уведомления прочитанным — глушит ошибки без логирования.
- **Магические строки** и hardcoded-тексты разбросаны по коду (`"QR-коды:"`, `"Неизвестная команда"`, `"отметить прочитанным"` и т.п.) — нет централизованных констант/i18n.
- **`TelegramBotService` напрямую ходит в `NotificationRepository`** — нарушение слоя (бот должен быть тонким транспортом).
- **Тестов на модуль нет.**
- `JWT без expiration` (общий для проекта) не относится к боту напрямую, но делает авторизацию в боте (через `telegram_dialog_id`) тоже «вечной».

См. план рефакторинга: `review/2026-04-21_TELEGRAM_REFACTORING.md` *(будет создан)*.

## Ссылки

- Связанные фичи: [F5](../FEATURES.md) (уведомления), [F2](../FEATURES.md) (QR), [F3](../FEATURES.md) (временные QR), [F6](../FEATURES.md) (настройки каналов), [F12](../FEATURES.md) (feedback).
- Код: `backend/src/main/java/ru/car/service/message/telegram/`.
- Интерфейс канала: `backend/src/main/java/ru/car/service/message/Sender.java`.
