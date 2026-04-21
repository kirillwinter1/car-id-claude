# F7: Push-уведомления (Firebase FCM)

**Статус:** ✅ В проде · **Последний апдейт:** 2026-04-21

## Что делает

Отправка push-уведомлений в мобильное приложение через Firebase Cloud Messaging (FCM HTTP v1). Каждое устройство при логине сообщает свой FCM-токен; backend хранит токены и при событии рассылает push на **все** устройства пользователя.

## Сценарии

### Регистрация токена устройства

1. Mobile после логина или обновления FCM-токена → `POST /api/notification_settings.update_token { token }`.
2. `FirebaseTokenService.createOrUpdateToken` ищет запись по `auth_id` (устройство/сессия); обновляет `token`, или создаёт новую запись с `user_id + auth_id + token`.

### Отправка push

1. Событие: `MessageService.sendPush(NotificationDto)` (при создании уведомления, [F4](../FEATURES.md), [F5](../FEATURES.md)).
2. Собирается `TextMessage` с `firebaseTokens = FirebaseService.getFirebaseTokens(userId)` — все токены пользователя.
3. `MessageService.asyncSend` → `trySend(firebaseService, message)`.
4. `FirebaseService.sendNotification` итерирует по всем токенам, шлёт `POST {firebase.url}` с OAuth2 bearer (`GoogleCredentials.refreshIfExpired`), payload — title = `CAR_ID_TITLE`, body = текст, `apns.payload.aps.sound = default`.
5. Успех, если хоть один токен доставил (`success |=`).

### Удаление токенов

- При `user.logout` → `firebaseTokenRepository.deleteByAuthId(authId)` — удаляются токены только этого устройства.
- При удалении пользователя → `deleteAllByUserId(userId)` — все токены.

### Hello push новому пользователю

1. После подтверждения кода `LoginAuthMobileFacade.confirmCode` вызывает `messageService.sendHelloNewUser(phoneNumber)`.
2. Через 30 сек (`CompletableFuture.delayedExecutor`) → `firebaseService.sendNotification` с текстом `HELLO_PUSH_MESSAGE` ([F1](../FEATURES.md)).

## API / Интеграция

**Backend endpoint:**
- `POST /api/notification_settings.update_token` — регистрация FCM токена.

**Внешний сервис:** Firebase Cloud Messaging HTTP v1.
- URL: `firebase.url` = `https://fcm.googleapis.com/v1/projects/car-id-55917/messages:send`.
- Авторизация: OAuth2 service account (`firebase.sourceCredentials` — JSON-файл в ресурсах).
- Scope: `firebase.scopeCredentials` = `https://www.googleapis.com/auth/firebase.messaging`.

**Payload:**
```json
{
  "message": {
    "token": "<fcm-token>",
    "notification": { "title": "Car-ID", "body": "<text>" },
    "apns": { "payload": { "aps": { "sound": "default" } } }
  }
}
```

## Реализация

**Backend:**
- `service/message/firebase/FirebaseService` — `implements Sender`. `@PostConstruct init()` загружает `GoogleCredentials`, `sendNotification` итерирует по токенам, `canSendNotification` = `pushEnabled`.
- `service/message/firebase/FirebaseProperties` — `@ConfigurationProperties("firebase")`.
- `service/message/firebase/Push*.java` — POJO для сериализации payload'а (Push, PushMessage, PushNotification, PushApns, PushPayload, PushAps, PushResponse).
- `service/FirebaseTokenService` — create-or-update токенов по `auth_id`.
- `repository/FirebaseTokenRepository` — JdbcTemplate (deleteByAuthId, deleteAllByUserId, findAllByUserId, findByAuthId, save, updateTokenById).

**Mobile:** Firebase SDK (см. `mobile/lib/...`), при получении токена → вызов `notification_settings.update_token`.

**БД (таблица `firebase_token`):** `id`, `auth_id` (устройство/сессия), `user_id`, `token`.

**Конфигурация:**
- `firebase.url`, `firebase.scopeCredentials`, `firebase.sourceCredentials` — путь к json с service account.
- В prod — `sourceCredentials` должен указывать на реальный ключ (в репозитории может отсутствовать).

## Ограничения / известный техдолг

- **`RestTemplate` создаётся на каждый send** внутри приватного метода `send` — не пулится, не имеет таймаутов по умолчанию.
- **`main(String[])` метод в production-классе** — остаток ручного теста с захардкоженным токеном и реальным сервис-аккаунтом (`car-id-55917-601a5782f3a2.json`). Следует удалить.
- **`catch (Exception ignored) {}`** в `sendNotification` — если один токен падает, другие шлются, но ошибка не логируется.
- **Нет инвалидации устаревших токенов:** FCM возвращает `UNREGISTERED` / `INVALID_ARGUMENT` для мёртвых токенов. Код не чистит БД — таблица растёт бесконечно.
- **Нет retry с backoff** — один provisional fail = потеря уведомления.
- **Title хардкодится** константой `CAR_ID_TITLE` — при ребрендинге нужно менять в коде.
- **`sendHelloNewUser`** идёт только через Firebase (хоть называется `trySend`) — если пользователь не включил push, hello не дойдёт.

## Ссылки

- Связанные фичи: [F1](../FEATURES.md) (hello push новому пользователю), [F5](../FEATURES.md) (ответный push при прочтении), [F6](../FEATURES.md) (`pushEnabled`, update_token endpoint).
- Код: `backend/src/main/java/ru/car/service/message/firebase/`, `backend/src/main/java/ru/car/service/FirebaseTokenService.java`.
