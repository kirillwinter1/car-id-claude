# F10: SMS (SMS Aero)

**Статус:** ⚠️ Частично используется · **Последний апдейт:** 2026-04-21

## Что делает

SMS-канал через провайдера [SMS Aero](https://smsaero.ru). Формально `SmsService implements Sender`, т.е. может использоваться как канал доставки уведомлений. Реально в текущем коде используется как **резервный канал для кода авторизации**: через 30 сек после flashcall, если код не введён, SMS дублирует его.

## Сценарии

### SMS с кодом авторизации (резерв flashcall)

1. `LoginAuthMobileService.loginMobile` → `MessageService.sendFlashcallCode(phone)` → Zvonok flashcall ([F11](../FEATURES.md)).
2. Тот же метод планирует `CompletableFuture.delayedExecutor(30s)` → если код ещё в БД (`authenticationCodeService.existsCode`), значит не введён → `smsService.send(phone, text)`.
3. SMS содержит код + приветствие: `"Код для доступа в Car-ID: <код>"`.

### (Формально) Уведомления

- `SmsService.canSendNotification` возвращает `true` для всех — формально всегда пытается отправить, но `MessageService.asyncSend` **не** включает `smsService` в рассылку (только Firebase + Telegram + отложенный Zvonok).
- Итог: SMS как канал уведомлений о событиях на QR **не включён в текущем коде**.

## API / Интеграция

**Внешний сервис:** SMS Aero HTTP API v2.
- Базовые URL (fallback-список): `https://gate.smsaero.ru/v2/`, `.org`, `.net`, `.uz`.
- Авторизация: Basic `email:apiKey`.
- Метод: `sms/send`, дополнительно `balance`, `sms/status`, `viber/send`, и пр.

**Клиент** — свой `SmsAeroClient` (не зависимость, а скопированный файл), 270 строк.

## Реализация

**Backend:**
- `service/message/sms/SmsService` — `implements Sender`. Проверяет баланс перед отправкой, шлёт через `SmsAeroClient.Send`.
- `service/message/sms/SmsAeroClient` — ручная реализация HTTP-клиента с fallback по зеркалам доменов.
- `service/MessageService.sendFlashcallCode` — планирует SMS через 30 сек как резерв.

## Ограничения / известный техдолг

- **Credentials захардкожены в коде** (`SmsService.java`):
  - `email = "t89043139690@mail.ru"`
  - `apiKey = "-6nLeBPieJi8RsI_lv5dY2pZrz1Esm1T"`
  - `sign = "SmsAero"`

  Это **секрет в репозитории**, его нужно вынести в конфиг с Jasypt и ротировать.
- **Проверка баланса == 0.00** сравнивает `Double` через `.equals(0.00)` — хрупко, не поймает 0.01.
- **`System.out.println("Insufficient balance")`** — логирование через stdout в production-коде.
- **`SmsAeroClient`** — 270 строк скопированного кода, большая часть методов (Viber, ContactList, HlrCheck, GroupAdd и т.д.) не используется.
- **`SmsAeroClient._email, _apiKey, _baseDomain` — `static`** → проблема concurrency при параллельных вызовах с разных инстансов (впрочем, тут всегда одни и те же значения).
- **`canSendNotification` возвращает `true` всегда** — нарушение контракта `Sender`; если бы SMS вернули в веер, получали бы SMS все независимо от настроек.
- **Нет `smsEnabled` флага** в `NotificationSetting` — канал нельзя выключить с клиента.
- **`sign = "SmsAero"`** — отправляется под дефолтной подписью; у пользователя может отображаться как спам.

## Ссылки

- Связанные фичи: [F1](../FEATURES.md) (резерв для кода авторизации), [F11](../FEATURES.md) (основной канал — flashcall).
- Код: `backend/src/main/java/ru/car/service/message/sms/`.
