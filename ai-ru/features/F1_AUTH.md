# F1: Auth по телефону (flashcall + SMS + JWT)

**Статус:** ✅ В проде · **Последний апдейт:** 2026-04-21

## Что делает

Мобильная авторизация по номеру телефона без пароля. Пользователь вводит телефон, получает **flashcall** (входящий звонок, последние 4 цифры номера = код), вводит код, получает **JWT-токен**. Регистрация происходит автоматически при первом подтверждении.

Админ-логин идёт по фиксированному номеру + коду из конфигурации (без звонка).

## Сценарии

### Обычный пользователь

1. Mobile: ввод телефона → `POST /api/user.login_oauth_mobile { phoneNumber }`.
2. Backend: проверяет, что код для этого номера не отправлялся последние `SMS_NEXT_REQUEST_TIMEOUT_IN_SEC` секунд, затем `messageService.sendFlashcallCode(phone)` → Zvonok делает flashcall, последние 4 цифры caller-id сохраняются как код.
3. Через 30 сек, если код не введён — SMS-резерв (см. `MessageService.sendFlashcallCode`).
4. Mobile: ввод кода → `POST /api/user.login_oauth_code { phoneNumber, code }`.
5. Backend: находит/создаёт `User` (роль `ROLE_USER`), создаёт запись в `notification_setting`, выдаёт **JWT без expiration**. Новому пользователю через 30 сек отправляется приветственный push (см. [F7](../FEATURES.md)).

### Админ

1. Телефон `admin.phone` + код `admin.code` из конфига → звонок не делается.
2. Роль `ROLE_ADMIN`, JWT тот же механизм.

### Logout

1. `POST /api/user.logout` → удаляются все `firebase_token` привязанные к `auth_id` (устройству); JWT токен клиент должен выбросить сам (сервер его не отзывает).

### Удаление аккаунта

1. `POST /api/user.delete` → `user.active = false`, каскадно удаляются `notification_setting`, QR-коды и Firebase-токены.

## API / Интеграция

| Endpoint | Описание |
|----------|----------|
| `POST /api/user.login_oauth_mobile` | Запросить flashcall с кодом |
| `POST /api/user.login_oauth_code` | Подтвердить код → JWT |
| `POST /api/user.logout` | Выход (инвалидация firebase_token по auth_id) |
| `POST /api/user.get` | Данные текущего пользователя |
| `POST /api/user.delete` | Удалить аккаунт (soft-delete `active=false`) |

**Callback flashcall (`ZvonokPostBackController`)** — Zvonok дёргает backend с `phoneFrom`/`phoneTo`, и `AuthenticationCodeService.createCodeFromCallerNumber` сохраняет последние 4 цифры как код. См. [F11](../FEATURES.md).

**JWT claims:** `subject = phone`, `id = userId`, `telephone`, `role`, `authId`. HS256.

## Реализация

**Backend:**
- `controller/LoginAuthMobileController` — endpoints запрос/подтверждение/логаут.
- `service/LoginAuthMobileService` — основная логика: rate-limit, отправка кода, подтверждение.
- `service/LoginAuthMobileFacade` — обёртка: если пользователь новый — отправить приветственный push.
- `service/AuthenticationCodeService` — CRUD кодов в таблице `authentication_code`, rate-limit по времени.
- `service/UserService.findOrCreateByPhoneNumberAndActivate` — авто-регистрация + реактивация удалённых.
- `service/security/JwtService` — генерация/парсинг JWT (HS256), ключ `token.signing.key`.
- `service/security/AuthService` — достаёт `userId`/`authId`/phone из `SecurityContextHolder`.
- `service/security/SecurityUserService` — загрузка `SecurityUser` (Spring Security `UserDetails`).
- `controller/UserController` — `/user.get`, `/user.delete`.

**Mobile:** `lib/screens/auth/` — экраны ввода телефона и кода.

**БД:**
- `users` — основная таблица пользователей (role, phone, active).
- `authentication_code` — отправленные коды, для rate-limit и подтверждения.
- `auth` (`auth_id`) — устройство/сессия, к которой привязаны firebase_token.

**Конфигурация:**
- `admin.phone`, `admin.code` — аварийный вход без звонка.
- `token.signing.key` — base64 HS256 key (в prod через Jasypt).
- `ApplicationConstants.SMS_NEXT_REQUEST_TIMEOUT_IN_SEC` — rate-limit.

## Ограничения / известный техдолг

- **JWT без expiration** — строка `setExpiration` закомментирована в `JwtService.generateToken`. Токен валиден вечно.
- **`isTokenValid` не проверяет срок** — метод `isTokenExpired` написан, но не вызывается.
- **`AuthenticationCodeService.isAlreadySent`** считает `LocalTime.now().minusSeconds(...)` — на переходе через полночь даст отрицательное время; баг в окне около 00:00.
- **Нет отзыва токена на стороне сервера** — logout удаляет только push-токены.
- **Flashcall без таймаута на код** — код в таблице живёт до следующего удаления, если его не ввели.
- **Админ-телефон/код в открытом конфиге** в dev-профиле (`admin.code: 1111`).

## Ссылки

- Связанные фичи: [F6](../FEATURES.md) (настройки каналов — создаются при регистрации), [F7](../FEATURES.md) (hello push новому пользователю), [F11](../FEATURES.md) (Zvonok flashcall), [F10](../FEATURES.md) (SMS fallback).
- Код: `backend/src/main/java/ru/car/service/LoginAuthMobile*.java`, `backend/src/main/java/ru/car/service/security/`, `backend/src/main/java/ru/car/controller/LoginAuthMobileController.java`.
