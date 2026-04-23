# F8: Telegram-бот (уведомления + команды)

**Статус:** ✅ В проде · **Последний апдейт:** 2026-04-23 (Phase 2.3 — Report/Support/Marketplace)

## Что делает

Telegram-бот решает две задачи:

1. **Канал доставки уведомлений** — если пользователь привязал Telegram в настройках ([F6](../FEATURES.md)), входящие события по его QR-кодам приходят ему в личку с кнопкой «отметить прочитанным».
2. **Копия мобильного приложения** (Phase 2.2 + 2.3) — главный экран со счётчиками, список меток с деталями и PNG QR-кодом, список уведомлений с вкладками/пагинацией/фильтром по QR, настройки каналов (три свитча), профиль (выход/удаление аккаунта), временный QR (PNG), **сообщение о событии** (выбор QR → причина → комментарий → отправка), **поддержка** (одношаговый feedback через `FeedbackChannels.TELEGRAM`), **маркетплейсы** (ссылки Wildberries/Ozon). Навигация edit-in-place, HTML + эмодзи, `← Назад` через `parentKey()`. Многошаговые формы через `SceneStateRegistry` (in-memory, TTL 5 мин).

В продакшене — `@car_id_ru_bot`, в dev — `@car_id_test_bot`. Запуск — long polling (не webhook).

## Сценарии

### Привязка Telegram к аккаунту

1. Пользователь пишет боту → бот видит, что `telegram_dialog_id` незнаком.
2. Бот запрашивает контакт (кнопка «поделиться контактом»).
3. Если телефон есть в таблице `users` — записывается `notification_setting.telegram_dialog_id = chatId`, выдаётся приветствие; reply-клавиатура убирается через `ReplyKeyboardRemove`, роутер автоматически рендерит `HomeScene` отдельным сообщением.
4. Если телефона нет — бот предлагает скачать мобильное приложение.

### Получение уведомления

1. Backend отправил `sendPush` → `TelegramBotService.sendNotification(TextMessage)` через `MessageService.asyncSend`.
2. Сообщение приходит в чат с inline-кнопкой «отметить прочитанным» (callback = `notif:read:<uuid>`).
3. При клике: `TelegramRouter` → `NotificationMarkReadScene` вызывает `NotificationFacade.readBy()` и убирает кнопку.

### QR-коды через бота

1. Главный экран → кнопка «🚘 Мои метки · N» → `QrListScene` рендерит HTML-карточки со статусными эмодзи (🟢 активна, ⏸ отключена, 🕐 временная, ⚪ новая) и списком inline-кнопок.
2. Клик по карточке → `QrDetailsScene`: имя, seqNumber, статус, дата активации + кнопки «Показать код», «История уведомлений», «Отключить», «← К списку», «🏠 Главное».
3. «Показать код» → PNG через `QrUtils.generateQRCodeImage` (ZXing + логотип из `png/car-id.png`) с HTML-подписью «<b>{name}</b> · №{seq}». PDF больше не генерируется.
4. «Отключить» → подтверждение → `QrService.disable(id)` (soft-delete через `QrRepository.delete(qr)`).

### Уведомления через бота

1. Главный экран → «🔔 Уведомления · N новых» → `NotificationListScene` с вкладками (все/непрочитанные), пагинацией (5/стр), опциональным фильтром по QR (из `QrDetailsScene`).
2. Клик по элементу → делегируется в `NotificationMarkReadScene.renderNotification` — карточка + кнопка «отметить прочитанным».

### Настройки каналов

1. Главный экран → «⚙️ Настройки» → `NotificationSettingsScene` показывает текущее состояние трёх каналов (🔔 push / 📞 звонок / ✈️ telegram).
2. Клик по кнопке канала → `NotificationSettingService.toggleChannel(userId, channel)` инвертирует значение → перерендер.

### Профиль

1. Главный экран → «👤 Профиль» → `ProfileScene` показывает телефон (формат `+7 XXX XXX-XX-XX`) и роль.
2. «🚪 Выйти» → подтверждение → `NotificationSettingRepository.clearTelegramLink(userId)` обнуляет `telegram_enabled` и `telegram_dialog_id`.
3. «🗑 Удалить аккаунт» → подтверждение → `UserService.deleteUser(userId)`.

### Временный QR

1. Главный экран → «➕ Временный QR» → `TemporaryQrScene`: `QrService.createTemporaryQr(userId)` → PNG через `QrUtils.generateQRCodeImage` с caption, в котором ссылка `{url}/qr/{id}`. TTL ~[1h, 3h] (см. [F3](../FEATURES.md)).

### Сообщение о событии (Phase 2.3)

1. Главный экран → «📝 Сообщить о событии» → `ReportEventScene`: список активных/временных меток пользователя (через `QrRepository.findByUserId`).
2. Клик по метке → экран выбора причины (`ReasonDictionaryService.findAll()`).
3. Клик по причине → приглашение ввести текст; `SceneStateRegistry.put(chatId, "report", "text", [qrId, reasonId])`.
4. Пользователь пишет текст → роутер через pending-state делегирует в `ReportEventScene.handleText` → `SceneStateRegistry.updateDraft` + экран preview со всеми тремя полями и кнопками «✅ Отправить / ✏️ Изменить текст / ← Изменить причину».
5. «✅ Отправить» → `NotificationFacade.send(NotificationDto{qrId, reasonId, text, senderId=ctx.userId()})` → clear state → сообщение «✅ Событие отправлено».
6. Альтернативный вход: из `QrDetailsScene` → «📝 Сообщить о событии» → `report:start:<qrId>` → шаг 2 сразу.

### Поддержка (Phase 2.3)

1. Главный экран → «💬 Поддержка» → `SupportScene`: приглашение ввести текст; ставит pending-state.
2. Пользователь пишет сообщение → `handleText` → `FeedbackFacade.send(FeedbackDto{email="{phone}@telegram", text, channel=TELEGRAM})` → `MessageService.sendMail` → `TelegramBotService.sendFeedback` пересылает в admin-канал → clear state → сообщение «💬 Спасибо!».

### Маркетплейсы (Phase 2.3)

1. Главный экран → «🛒 Где купить стикеры» → `MarketplaceScene`: `MarketplaceService.get()` → HTML-экран с `InlineKeyboardButton.url(...)` на Wildberries/Ozon (Telegram открывает ссылки в браузере, не шлёт callback).
2. Если `activity=false` или оба URL пусты → сообщение «Пока недоступно».

### Админ-меню

**Удалено 2026-04-21 в Phase 2.1.** Мониторинг счётчиков (пользователи, активации QR, отправленные/прочитанные уведомления) теперь доступен через Prometheus/Grafana на проде.

## API / Интеграция

**Telegram Bot API** через `org.telegram.telegrambots` (long polling, `DefaultBotSession`).

**Callback-формат:** `<scene>:<action>[:<args>]`, 64 байта бюджет (Telegram). Парсер — `CallbackData.parse`. Служебное `<scene>:back` → роутер находит `parent = scene.parentKey()` и рендерит его edit-in-place.

**Ключи сцен и основные callbacks** (Phase 2.2 + 2.3):

| Scene | Key | Действия |
|-------|-----|---------|
| `HomeScene` | `home` | `home:open` |
| `QrListScene` | `qr_list` | `qr_list:open` |
| `QrDetailsScene` | `qr_details` | `qr_details:open:<uuid>`, `qr_details:show:<uuid>`, `qr_details:disable:<uuid>`, `qr_details:disable_confirm:<uuid>`, `qr_details:back` |
| `NotificationListScene` | `notif_list` | `notif_list:open:<tab>:<page>[:qr:<uuid>]`, `notif_list:view:<uuid>`, `notif_list:back` |
| `NotificationMarkReadScene` | `notif` | `notif:read:<uuid>` |
| `NotificationSettingsScene` | `settings` | `settings:open`, `settings:toggle:<push\|call\|telegram>`, `settings:back` |
| `ProfileScene` | `profile` | `profile:open`, `profile:logout`, `profile:logout_confirm`, `profile:delete`, `profile:delete_confirm`, `profile:back` |
| `TemporaryQrScene` | `temp_qr` | `temp_qr:create` |
| `ReportEventScene` | `report` | `report:start[:<uuid>]`, `report:qr:<uuid>`, `report:reason:<uuid>:<reasonId>`, `report:edit_text:<uuid>:<reasonId>`, `report:submit`, `report:back` |
| `SupportScene` | `support` | `support:start`, `support:back` |
| `MarketplaceScene` | `marketplace` | `marketplace:open`, `marketplace:back` (кнопки WB/Ozon — `url=`, callback'ов не шлют) |

**Multi-step формы** — через `SceneStateRegistry` (in-memory `ConcurrentHashMap<chatId, PendingText>`, TTL 5 мин). Роутер в `handleText` сначала смотрит `peek(chatId)`; если есть pending — делегирует в `scene.handleText(text, ctx, args)`. Используется в `ReportEventScene` (хранит qrId+reasonId между шагами выбора причины и ввода текста) и `SupportScene` (одношаговая форма).

reply-кнопка «поделиться контактом» (`tg.auth.btn.share_contact`) — единственная reply-клавиатура, живёт только до привязки; после привязки `ReplyKeyboardRemove`.

**Реализация интерфейса `Sender`** — бот участвует в веере каналов доставки через `MessageService.asyncSend` наряду с Firebase и Zvonok.

**Feedback-канал:** `TelegramBotService.sendFeedback(from, text)` — пересылает обратную связь из мобильного приложения в канал `telegram.feedbackChannelId` (см. [F12](../FEATURES.md)).

## Реализация

**Backend (пакет `ru.car.service.message.telegram`, Phase 2.1–2.3):**
- `TelegramBotService` — тонкий транспорт: `extends TelegramLongPollingBot implements Sender, TelegramTransport`. Делегирует входящие `Update` в `TelegramRouter`; исходящие уведомления рендерит через `NotificationMarkReadScene` → `TelegramRenderer`. Умеет `sendPhoto`.
- `TelegramRouter` (пакет `router/`) — entry-point: pre-auth (через `TelegramAuthorizationService`, после привязки автоматически рендерит `HomeScene`) | callback-роутинг (через `SceneRegistry` по `CallbackData.scene()`, служебное `:back` → родительская сцена через `parentKey()`) | text-триггеры (pending-state через `SceneStateRegistry.peek` → `scene.handleText`, иначе `SceneRegistry.findByText`). Fallback — `HomeScene.renderUnknown`.
- `TelegramAuthorizationService` (пакет `auth/`) — shareContact flow с привязкой `telegram_dialog_id`; welcome возвращает `ReplyKeyboardRemove`.
- `SceneRegistry` (пакет `scene/`) — Spring DI собирает `List<TelegramScene>` и мапит по `key()`.
- `SceneStateRegistry` (пакет `scene/state/`) — in-memory `ConcurrentHashMap<chatId, PendingText>` с TTL 5 мин для multi-step форм.
- Сцены (пакет `scene/impl/`): `HomeScene` (главное меню со счётчиками + fallback `renderUnknown`), `QrListScene` (HTML-карточки), `QrDetailsScene` (детали + PNG + disable + «сообщить о событии»), `NotificationListScene` (вкладки + пагинация + qr-фильтр), `NotificationMarkReadScene` (карточка уведомления), `NotificationSettingsScene` (три свитча), `ProfileScene` (телефон + выход + удаление), `TemporaryQrScene` (PNG), `ReportEventScene` (4 шага + pending-state), `SupportScene` (одношаговый feedback), `MarketplaceScene` (WB/Ozon URL-кнопки).
- `SceneOutput` (record) — `{text, inlineKeyboard, replyKeyboard, editInPlace, parseMode, photo, caption}`; фабрики `send/sendHtml/edit/editHtml/editMarkup/photo/noop`.
- `TelegramScene` — `key()`, `canHandleText()`, `render()`, `handle()`, `parentKey()` default = `"home"`, `handleText()` default = noop.
- `TelegramRenderer`, `TelegramMessages` (пакет `render/`) — сборка `SendMessage`/`EditMessageText`/`EditMessageReplyMarkup`/`SendPhoto` из `SceneOutput` + i18n обёртка над `MessageSource`.
- `TelegramTransport` (пакет `transport/`) — интерфейс транспорта (text + photo), реализован `TelegramBotService`.
- `TelegramConfig` — простой `@PostConstruct` с `bot.init()` (циклическая зависимость устранена, setter-hack удалён).
- `TelegramProperties` — `@ConfigurationProperties("telegram")`.
- `QrUtils.generateQRCodeImage` — PNG через ZXing с логотипом из `resources/png/car-id.png` (25 КБ).
- Тексты — в `backend/src/main/resources/i18n/telegram_ru.properties` (~160 ключей).

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

См. историю рефакторинга:
- Phase 2.1 (архитектура): [`review/2026-04-21_TG_2.1_ARCHITECTURE.md`](../review/2026-04-21_TG_2.1_ARCHITECTURE.md) · [`review/2026-04-21_TG_2.1_PLAN.md`](../review/2026-04-21_TG_2.1_PLAN.md).
- Phase 2.2 (базовые экраны): [`review/2026-04-21_TG_2.2_BASIC_SCREENS.md`](../review/2026-04-21_TG_2.2_BASIC_SCREENS.md) · [`review/2026-04-21_TG_2.2_PLAN.md`](../review/2026-04-21_TG_2.2_PLAN.md).
- Phase 2.3 (user actions): [`review/2026-04-21_TG_2.3_USER_ACTIONS.md`](../review/2026-04-21_TG_2.3_USER_ACTIONS.md) · [`review/2026-04-21_TG_2.3_PLAN.md`](../review/2026-04-21_TG_2.3_PLAN.md).
- Мастер-эпик: [`review/2026-04-21_TELEGRAM_EPIC.md`](../review/2026-04-21_TELEGRAM_EPIC.md).

## Ссылки

- Связанные фичи: [F5](../FEATURES.md) (уведомления), [F2](../FEATURES.md) (QR), [F3](../FEATURES.md) (временные QR), [F6](../FEATURES.md) (настройки каналов), [F12](../FEATURES.md) (feedback).
- Код: `backend/src/main/java/ru/car/service/message/telegram/`.
- Интерфейс канала: `backend/src/main/java/ru/car/service/message/Sender.java`.
