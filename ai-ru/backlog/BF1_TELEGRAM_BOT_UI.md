# BF1: Telegram-бот как второй клиент приложения

**Статус:** 🚧 In progress (Phase 2 в [ROADMAP.md](../ROADMAP.md)) — эпик из 5 под-спеков, 2.1/2.2/2.3/2.4 закрыты
**Последний апдейт:** 2026-04-23

> **⚠️ Скоуп пересмотрен 2026-04-21.** Изначально BF1 = «рефакторинг + HTML/эмодзи». По итогам брейнсторма превращён в эпик: **бот = копия мобильного приложения** (те же экраны, команды, переходы) + канал уведомлений + задел под Phase 5 (парковка). Полный план — в **[review/2026-04-21_TELEGRAM_EPIC.md](../review/2026-04-21_TELEGRAM_EPIC.md)**.
>
> **✅ Под-спек 2.1 закрыт 2026-04-21** — архитектура + Scene-скелет + i18n + удаление админ-меню. См. [review/2026-04-21_TG_2.1_ARCHITECTURE.md](../review/2026-04-21_TG_2.1_ARCHITECTURE.md) и [review/2026-04-21_TG_2.1_PLAN.md](../review/2026-04-21_TG_2.1_PLAN.md).
>
> **✅ Под-спек 2.2 закрыт 2026-04-23** — перенос базовых экранов: `HomeScene` со счётчиками, `QrListScene` v2 (HTML-карточки), `QrDetailsScene` (PNG вместо PDF + disable), `NotificationListScene` (вкладки + пагинация + qr-фильтр), `NotificationSettingsScene` (3 свитча), `ProfileScene` (телефон + выход + удаление), `TemporaryQrScene` v2 (PNG). Edit-in-place навигация через `parentKey()` + `:back`. Auth-welcome → `ReplyKeyboardRemove` + авто-рендер Home. Удалён легаси `HomeMenuScene`. См. [review/2026-04-21_TG_2.2_BASIC_SCREENS.md](../review/2026-04-21_TG_2.2_BASIC_SCREENS.md) и [review/2026-04-21_TG_2.2_PLAN.md](../review/2026-04-21_TG_2.2_PLAN.md).
>
> **✅ Под-спек 2.3 закрыт 2026-04-23** — user actions: `ReportEventScene` (многошаговая форма выбор QR → причина → текст → отправка через `NotificationFacade.send`), `SupportScene` (одношаговый feedback через `FeedbackFacade.send` с `FeedbackChannels.TELEGRAM`), `MarketplaceScene` (URL-кнопки на Wildberries и Ozon). `SceneStateRegistry` для multi-step форм (in-memory, TTL 5 мин). `TelegramScene.handleText` + роутер обрабатывает pending-text-state. См. [review/2026-04-21_TG_2.3_USER_ACTIONS.md](../review/2026-04-21_TG_2.3_USER_ACTIONS.md) и [review/2026-04-21_TG_2.3_PLAN.md](../review/2026-04-21_TG_2.3_PLAN.md).
>
> **✅ Под-спек 2.4 закрыт 2026-04-23** — deep-link онбординг: `TelegramStartTokenService` подписывает `userId:expiresAt` через HMAC-SHA256 (truncated до 80 бит, fits 64-char `/start` payload). `POST /api/telegram.get_start_url` возвращает мобилке `https://t.me/<bot>?start=<token>` (TTL 15 мин). Роутер отсекает префикс `/start `, `TelegramAuthorizationService` верифицирует токен и привязывает `telegram_dialog_id` без shareContact. Мобайл: `WidgetsBindingObserver` авто-рефрешит настройки при возврате, snackbar «✅ Telegram подключён». Fallback на shareContact остаётся для пришедших в бота напрямую. См. [review/2026-04-21_TG_2.4_DEEP_LINK.md](../review/2026-04-21_TG_2.4_DEEP_LINK.md) и [review/2026-04-21_TG_2.4_PLAN.md](../review/2026-04-21_TG_2.4_PLAN.md). Следующий под-спек — 2.5 (notification card rendering + reason emoji).

## Что делаем

Переводим текущего Telegram-бота ([F8](../features/F8_TELEGRAM_BOT.md)) из функционального состояния в **сильный канал** приложения: красивый UI, ощущение заботы, быстрый онбординг, эмоциональные сообщения.

## Почему именно сейчас, после стабилизации

- Telegram — самый дешёвый канал доставки (не требует Firebase, не платится за сообщение).
- Текущие архитектурные проблемы бота (A1–A4 в [TECH_DEBT.md](../TECH_DEBT.md)) всё равно надо закрывать перед большими изменениями UX — иначе каждая новая кнопка = ещё один case в мега-switch.
- Для Phase 3 (мульти-канальная авторизация через Telegram) нужен стабильный бот.

## Желаемые сценарии

### Онбординг

1. Пользователь нажимает кнопку «Настроить Telegram» в мобайле → получает `https://t.me/car_id_ru_bot?start=<user_id>` (deep-link).
2. Бот распознаёт `start` payload, связывает аккаунт без явной передачи контакта.
3. Приветственное сообщение: визитная карточка бренда, gif/картинка, краткий туториал («вот тут — ваши QR, вот тут — история уведомлений»).

### Уведомление

Текущее: `<qrName>:\n<reasonDescription>\n<custom text>`.

Желаемое:
```
🚨 Событие на автомобиле «Audi Q5»
├─ Причина: Работает сигнализация
├─ Комментарий: «Сработала, пошла 5 минут назад»
└─ 21 апреля, 10:35

[отметить прочитанным]  [перейти в приложение]
```

С эмодзи по типу события (🚨 ДТП, 🚗 мешает, 🔧 ремонт завершён, 🧼 мойка готова, ⚠️ сигнализация).

### Список QR

Вместо плоского списка inline-кнопок — карточка на каждый QR:

```
🚘 Audi Q5 (№ 145)
Статус: активен
Уведомлений: 12 (2 непрочитанных)

[скачать QR PDF]  [история]  [отключить]
```

### Быстрые действия

- `/newqr` — создать временный QR без входа в приложение.
- `/stats` — личная статистика (сколько уведомлений получил, сколько отправил).
- `/support` — быстрая связь.

### Админ-меню (для `user.is_admin = true`)

Сейчас — плоский `/registerUserCount` и пр. Желаемое — inline-кнопки с графиком недели, сегодня vs вчера.

## План реализации (верхнеуровнево)

1. **Сначала рефакторинг** ([TECH_DEBT A1–A4](../TECH_DEBT.md)):
   - Разорвать цикл `TelegramBotService ↔ TelegramLogicService` (убрать `@Setter`, разнести ответственности).
   - Вынести роутинг команд в Command-pattern.
   - Убрать `TelegramBotService` из обращения к `NotificationRepository` (через сервисный слой).
   - Централизовать все тексты в `TelegramMessages` (i18n-готовность).
2. **Тесты** — юнит на роутер команд, на рендер сообщений.
3. **UI-работа** — итеративно, с каждым рефакторингом сразу вводить новые шаблоны сообщений.
4. **Deep-link онбординг** — добавить в мобайл кнопку «Настроить Telegram» + логика связывания по `start` payload.

## Связанное

- Карточка существующего бота: [F8](../features/F8_TELEGRAM_BOT.md).
- Техдолг архитектуры: [TECH_DEBT.md A1–A4](../TECH_DEBT.md).
- Дальнейшее расширение — Phase 3 (Telegram как auth-канал) → [BF2](BF2_MULTI_CHANNEL_AUTH.md).
