# F6: Настройки каналов уведомлений

**Статус:** ✅ В проде · **Последний апдейт:** 2026-04-21

## Что делает

Одна строка `notification_settings` на пользователя с тремя флагами — какие каналы включены: **push** (Firebase), **call** (Zvonok), **telegram**. Плюс `telegram_dialog_id` для идентификации пользователя в Telegram-боте.

Мобильное приложение управляет этими флагами, backend валидирует: нельзя включить Telegram без привязки бота.

## Сценарии

### Автосоздание при регистрации

1. `UserService.findOrCreateByPhoneNumberAndActivate` — при первой авторизации → `NotificationSettingService.create(userId)`.
2. Дефолты: `push=true`, остальные `false`, `active=true`.

### Получение текущих настроек

1. `POST /api/notification_settings.get` → `NotificationSettingDto`.

### Обновление

1. `POST /api/notification_settings.patch` с изменяемыми полями — PATCH, null-поля игнорируются (`updateIgnoreNull` mapper).
2. **Валидация Telegram:** если `telegramEnabled` меняется на `true`, но `telegram_dialog_id == null` → `TELEGRAM_AUTH_ERROR` с сообщением, содержащим имя бота и `CONTACT_CMD`. Клиент показывает инструкцию «напиши боту и поделись контактом» ([F8](../FEATURES.md)).

### Обновление Firebase-токена устройства

1. `POST /api/notification_settings.update_token { token }` → создаёт/обновляет запись в таблице `firebase_token` (по `auth_id` устройства). Это отдельно от флагов, но живёт в том же контроллере. См. [F7](../FEATURES.md).

### Удаление пользователя

1. `UserService.deleteUser` → `deleteByUserId` → `active=false`, все каналы сброшены в дефолт, `telegram_dialog_id` обнулён.

## API / Интеграция

| Endpoint | Описание |
|----------|----------|
| `POST /api/notification_settings.get` | Получить настройки |
| `POST /api/notification_settings.patch` | Изменить часть полей |
| `POST /api/notification_settings.update_token` | Обновить Firebase token (см. [F7](../FEATURES.md)) |

## Реализация

**Backend:**
- `controller/NotificationSettingController` — три endpoint'a.
- `service/NotificationSettingService` — create / get / patch / delete + валидация Telegram.
- `mapper/NotificationSettingDtoMapper` — `updateIgnoreNull` для PATCH.
- `service/message/telegram/TelegramMenu.CONTACT_CMD` + `TelegramProperties.bot` — подставляются в текст ошибки.

**Mobile:** настройки в профиле приложения (переключатели по каналам push/call/telegram).

**БД (таблица `notification_settings`):**
- `user_id` (FK) — уникальный.
- `push_enabled`, `call_enabled`, `telegram_enabled boolean not null default false`.
- `telegram_dialog_id bigint null` — ставится Telegram-ботом при авторизации ([F8](../FEATURES.md)).
- `active boolean` — false при удалении аккаунта.

## Ограничения / известный техдолг

- **`deleteByUserId` не выполняет delete, а делает update** на дефолт. Имя метода вводит в заблуждение.
- **Mail и SMS как каналы не представлены в настройках** — SMS используется только как резерв для кодов авторизации ([F10](../FEATURES.md)); email-канала для уведомлений нет.
- **Отсутствует отдельный флаг `smsEnabled`, `emailEnabled`** — расширение потребует миграции.
- **Валидация Telegram требует, чтобы пользователь сначала написал боту** — нет ссылочного flow «нажми кнопку → получишь deep-link в бот с предзаполненным контактом».

## Ссылки

- Связанные фичи: [F1](../FEATURES.md) (создание при регистрации), [F7](../FEATURES.md) (Firebase token), [F8](../FEATURES.md) (привязка Telegram), [F11](../FEATURES.md) (canSend для Zvonok).
- Код: `backend/src/main/java/ru/car/controller/NotificationSettingController.java`, `backend/src/main/java/ru/car/service/NotificationSettingService.java`, `backend/src/main/java/ru/car/model/NotificationSetting.java`.
