# F8: Telegram-бот (уведомления + команды)

**Статус:** ✅ В проде · **Последний апдейт:** 2026-04-21 (Phase 2.1 рефакторинг)

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
2. Сообщение приходит в чат с inline-кнопкой «отметить прочитанным» (callback = `notif:read:<uuid>`).
3. При клике: `TelegramRouter` → `NotificationMarkReadScene` вызывает `NotificationFacade.readBy()` и убирает кнопку.

### QR-коды через бота

1. Пользователь жмёт кнопку «QR-коды» → бот отвечает списком inline-кнопок (имя + серийный номер).
2. Клик по QR → асинхронная генерация PDF (`PdfConverter.create(SvgUtils.getSvgText)`) → отправка документом `qr.pdf`.

### Временный QR

1. Кнопка «Временный QR» → `QrService.createTemporaryQr(userId)` → возвращает ссылку `{url}/qr/{id}` с TTL 2 часа (см. [F3](../FEATURES.md)).

### Админ-меню

**Удалено 2026-04-21 в Phase 2.1.** Мониторинг счётчиков (пользователи, активации QR, отправленные/прочитанные уведомления) теперь доступен через Prometheus/Grafana на проде.

## API / Интеграция

**Telegram Bot API** через `org.telegram.telegrambots` (long polling, `DefaultBotSession`).

**Команды-кнопки** (константы в `scene/impl/*` + конфиг i18n):

| Источник | Значение | Где живёт |
|----------|----------|-----------|
| reply-кнопка «QR-коды» | `tg.home.btn.qrs` | `HomeMenuScene.mainKeyboard()` |
| reply-кнопка «Временный QR» | `tg.home.btn.temp_qr` | `HomeMenuScene.mainKeyboard()` |
| reply-кнопка «поделиться контактом» | `tg.auth.btn.share_contact` | `TelegramAuthorizationService.contactKeyboard()` |
| callback `qr:list` | — | `QrListScene.ACTION_LIST` |
| callback `qr:pdf:<uuid>` | — | `QrListScene.ACTION_PDF` |
| callback `notif:read:<uuid>` | — | `NotificationMarkReadScene.ACTION_READ` |

**Реализация интерфейса `Sender`** — бот участвует в веере каналов доставки через `MessageService.asyncSend` наряду с Firebase и Zvonok.

**Feedback-канал:** `TelegramBotService.sendFeedback(from, text)` — пересылает обратную связь из мобильного приложения в канал `telegram.feedbackChannelId` (см. [F12](../FEATURES.md)).

## Реализация

**Backend (пакет `ru.car.service.message.telegram`, после рефакторинга Phase 2.1):**
- `TelegramBotService` — тонкий транспорт: `extends TelegramLongPollingBot implements Sender, TelegramTransport`. Делегирует входящие `Update` в `TelegramRouter`; исходящие уведомления рендерит через `NotificationMarkReadScene` → `TelegramRenderer`.
- `TelegramRouter` (пакет `router/`) — entry-point: pre-auth (через `TelegramAuthorizationService`) | callback-роутинг (через `SceneRegistry` по `CallbackData.scene()`) | text-триггеры (через `SceneRegistry` по `canHandleText`).
- `TelegramAuthorizationService` (пакет `auth/`) — shareContact flow с привязкой `telegram_dialog_id`.
- `SceneRegistry` (пакет `scene/`) — Spring DI собирает `List<TelegramScene>` и мапит по `key()`.
- Сцены (пакет `scene/impl/`): `HomeMenuScene` (fallback + reply-клавиатура), `QrListScene`, `TemporaryQrScene`, `NotificationMarkReadScene`.
- `TelegramRenderer`, `TelegramMessages` (пакет `render/`) — сборка `SendMessage`/`EditMessage` из `SceneOutput` + i18n обёртка над `MessageSource`.
- `TelegramTransport` (пакет `transport/`) — интерфейс транспорта, реализован `TelegramBotService`.
- `TelegramConfig` — простой `@PostConstruct` с `bot.init()` (циклическая зависимость устранена, setter-hack удалён).
- `TelegramProperties` — `@ConfigurationProperties("telegram")`.
- Тексты — в `backend/src/main/resources/i18n/telegram_ru.properties`.

**БД (таблица `notification_setting`):**
- `telegram_enabled boolean not null default false` — канал включён.
- `telegram_dialog_id bigint null` — chatId пользователя; по нему бот определяет, кто пишет.

**Конфигурация (`application*.yml`):**
- `telegram.bot`, `telegram.token`, `telegram.feedbackChannelId`, `telegram.enable`.
- В prod — токен зашифрован Jasypt.

## Ограничения / известный техдолг

- **Тестов на `TelegramBotService` напрямую нет** — он тонкий транспорт, покрытие через сцены (интеграционное тестирование бота отложено).
- **`TelegramBotService` совмещает `Sender` и `TelegramTransport`** — привело к 3 `@Lazy`-инъекциям на сценах/рендерере для разрыва DI-цикла. Кандидат на последующий refactor (см. [TECH_DEBT A19](../TECH_DEBT.md)).
- **JWT без expiration** (общий для проекта) — авторизация бота тоже «вечная» через `telegram_dialog_id`.

См. историю рефакторинга: [`review/2026-04-21_TG_2.1_ARCHITECTURE.md`](../review/2026-04-21_TG_2.1_ARCHITECTURE.md), план: [`review/2026-04-21_TG_2.1_PLAN.md`](../review/2026-04-21_TG_2.1_PLAN.md).

## Ссылки

- Связанные фичи: [F5](../FEATURES.md) (уведомления), [F2](../FEATURES.md) (QR), [F3](../FEATURES.md) (временные QR), [F6](../FEATURES.md) (настройки каналов), [F12](../FEATURES.md) (feedback).
- Код: `backend/src/main/java/ru/car/service/message/telegram/`.
- Интерфейс канала: `backend/src/main/java/ru/car/service/message/Sender.java`.
