# F11: Flashcall / голосовой звонок (Zvonok)

**Статус:** ✅ В проде · **Последний апдейт:** 2026-04-21

## Что делает

Интеграция с сервисом [Zvonok](https://zvonok.com) для двух целей:

1. **Flashcall-авторизация** — вместо SMS: пользователь получает короткий звонок с неизвестного номера, последние 4 цифры caller-id являются кодом подтверждения. Нулевой SMS-трафик, быстрее, дешевле.
2. **Голосовой звонок с уведомлением** — робот зачитывает текст уведомления («Вам звонок из car-id…»). Используется как «крайняя мера» через 30 секунд после push, если уведомление не прочитано.

## Сценарии

### Flashcall-код

1. `LoginAuthMobileService.loginMobile` → `MessageService.sendFlashcallCode` → `ZvonokService.sendCode(phone)`.
2. `POST {zvonok.urlFlashcall}` с `campaign_id=flashcallCampaignId`, `phone`, `public_key`.
3. Zvonok возвращает `FlashcallDto.data.pincode` — последние 4 цифры номера, с которого будет звонок.
4. Backend сохраняет код в `authentication_code` через `AuthenticationCodeService.create`.
5. Zvonok звонит, сбрасывает сразу → пользователь видит caller-id, вводит последние 4 цифры.

### Callback от Zvonok (flashcall)

- `GET /api/zvonok/success?phoneFrom=&phoneTo=&callId=&campaignId=` — успешный дозвон. Если campaign = `codeCallCampaignId`, backend через `AuthenticationCodeService.createCodeFromCallerNumber` на всякий случай тоже сохраняет код (дублирует то, что уже положил `sendCode`, но с учётом того, что номер Zvonok может отличаться между campaign-серверами).
- `GET /api/zvonok/fail` — тот же callback на случай провала, та же логика (запасной вариант).

### Голосовой звонок с уведомлением

1. `MessageService.asyncSend` веерно шлёт push/telegram.
2. Через 30 сек → проверка `notificationService.isUnread(id)` → если всё ещё `UNREAD` и канал включён (`callEnabled`) → `trySend(zvonokService, message)`.
3. `ZvonokService.sendNotification`: `sendMessage(phone, "Вам звонок из car id . <text>")` → `POST {urlCall}` с `campaign_id=callCampaignId`.
4. В ответе `CallData.call_id` → `notificationService.updateCallId(notificationId, callId)` — связывает уведомление с конкретным звонком.

### Callback от Zvonok (mark as read)

1. Пользователь **перезванивает** по пропущенному звонку → Zvonok дёргает `GET /api/zvonok/notification/markAsRead?callId=...`.
2. `NotificationService.readByCallId(callId)` → уведомление → статус `READ` ([F5](../FEATURES.md)).

### (Альтернативный путь) Голосовой код вместо flashcall

- `ZvonokService.sendCodeMessage` — робот проговаривает код голосом («Код для доступа: 1 2 3 4, ещё раз: 1 2 3 4»). Используется в `MessageService.sendCallCode` как резерв через 30 сек.

## API / Интеграция

**Внешний сервис:** Zvonok API (form-urlencoded POST).

| URL | Назначение | Параметры |
|-----|-----------|-----------|
| `zvonok.urlFlashcall` | Flashcall с пинкодом | `campaign_id=flashcallCampaignId`, `phone`, `public_key` |
| `zvonok.urlCall` | Голосовой звонок | `campaign_id=callCampaignId` или `codeCallCampaignId`, `phone`, `public_key`, `text` |

**Backend endpoints для callback'ов (анонимные GET):**
| Endpoint | Назначение |
|----------|-----------|
| `GET /api/zvonok/success` | Успешный дозвон (для flashcall-campaign) |
| `GET /api/zvonok/fail` | Неуспешный дозвон |
| `GET /api/zvonok/notification/markAsRead` | Перезвон владельца = подтверждение прочтения |

## Реализация

**Backend:**
- `service/message/zvonok/ZvonokService` — `implements Sender`. Три метода отправки (`sendCode`, `sendCodeMessage`, `sendMessage`) + `sendNotification` для канала уведомлений.
- `service/message/zvonok/ZvonokProperties` — `@ConfigurationProperties("zvonok")`.
- `service/message/zvonok/FlashcallDto`, `CallData` — ответы от Zvonok API.
- `controller/ZvonokPostBackController` — 3 callback endpoint'a.
- `service/AuthenticationCodeService.createCodeFromCallerNumber` — вычленяет последние 4 цифры из caller phone.
- `service/NotificationService.readByCallId` — пометка прочтения по `call_id`.

**БД (таблица `notification`):** поле `call_id bigint null` — заполняется при голосовом уведомлении.

**Конфигурация:**
- `zvonok.publicKeyApi` — ключ API (Jasypt).
- `zvonok.flashcallCampaignId` — campaign для flashcall-кода.
- `zvonok.codeCallCampaignId` — campaign для голосового проговаривания кода.
- `zvonok.callCampaignId` — campaign для уведомлений.

## Ограничения / известный техдолг

- **Callback-endpoints не аутентифицированы.** Любой, кто знает `campaign_id`, может отправить `markAsRead` на произвольный `callId`. Zvonok не подписывает запросы по умолчанию. Риск: спам/подделка пометки прочтения.
- **`getSuccessCall` и `getFailCall` делают одно и то же** — создают код из caller phone. Разница только в логах. Зачем-то два endpoint'а.
- **В коде закомментирован URL тестового домена** `gachi-huyachi.fun` — следует удалить.
- **RestTemplate создаётся как поле `= new RestTemplate()`** — без пула и таймаутов, разделён между вызовами (OK для RestTemplate, но конфигурация не гибкая).
- **`sendCodeMessage` возвращает `boolean`, `sendMessage` возвращает `CallData`, `sendCode` возвращает `String`** — три разных API, один класс; пора выделить в отдельные сервисы по назначению.
- **Нет обработки кода ошибки от Zvonok** (например «нет баланса») — при 2xx, но `status != "ok"` `sendCode` кидает `MessageNotSendException`, но другие методы молча логируют и возвращают `false`/`null`.
- **`text = "Вам звонок из car id . " + message.getText()"`** — хардкод шаблона, без возможности кастомизации.

## Ссылки

- Связанные фичи: [F1](../FEATURES.md) (flashcall-авторизация), [F5](../FEATURES.md) (mark as read по callId), [F6](../FEATURES.md) (`callEnabled`), [F10](../FEATURES.md) (SMS-резерв).
- Код: `backend/src/main/java/ru/car/service/message/zvonok/`, `backend/src/main/java/ru/car/controller/ZvonokPostBackController.java`.
