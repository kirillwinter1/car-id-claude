# BF2: Мульти-канальная авторизация и уведомления

**Статус:** 📋 Planned (Phase 3 в [ROADMAP.md](../ROADMAP.md))
**Последний апдейт:** 2026-06-27 (добавлен дизайн VK ID — бэкенд)

> **Уточнение 2026-06-27.** Под «Telegram-входом» есть **два разных механизма**, не путать:
> - **Telegram Gateway API** — код подтверждения приходит в Telegram **по номеру телефона** (идентичность остаётся = телефон). Дешёвая замена SMS/flashcall, ложится на текущий телефонный вход, может выехать **независимо** от JWT-редизайна (P1). Детальный дизайн — раздел [«Telegram Gateway»](#telegram-gateway-api--код-по-номеру-near-term) ниже. **Это — ближайший инкремент.**
> - **Telegram Login Widget / Bot** — вход по самому аккаунту Telegram (deep-link, 2 тапа, без ввода кода и без телефона). Это исходный план в таблице ниже; остаётся на потом.

## Что делаем

Снимаем зависимость от платного flashcall через Zvonok как единственного способа входа. Добавляем более дешёвые и удобные для пользователя каналы и одновременно закрываем критический техдолг [P1 (ротация JWT signing key и секреты на диске)](../TECH_DEBT.md).

## Мотивация

- **Цена.** Flashcall = ₽ за каждую авторизацию. 1830 активных пользователей, плюс каждая регистрация — деньги. Другие каналы (Telegram Login, MAX) бесплатны или дешевле.
- **UX.** Звонок с неизвестного номера — уже не самый удобный способ. Telegram-login — 2 тапа без ввода кода.
- **Резерв.** Zvonok может упасть, SMS Aero — тоже. Альтернативный канал = надёжность.
- **Подготовка фич.** Аренда парковочных мест ([BF4](BF4_PARKING_RENTAL_EPIC.md)) тоже пойдёт через Telegram — логично иметь авторизацию туда же.

## Желаемые каналы авторизации

| Канал | Как работает | Плюсы | Минусы |
|-------|--------------|-------|--------|
| **Telegram Login Widget / Bot** | Приложение открывает t.me/car_id_ru_bot с deep-link → бот подтверждает вход | Бесплатно, 2 тапа, уже есть бот | Нужен установленный Telegram |
| **MAX messenger** | API MAX для login | Российская аудитория, бесплатно | API ещё развивается, ожидаем документацию |
| **Flashcall (Zvonok) — текущий** | Остаётся как опорный fallback | Работает у всех | Платно |
| **SMS (SMS Aero) — текущий** | Fallback внутри flashcall | Универсально | Платно, часто в спам |
| **VK ID / Яндекс ID** | OAuth через их SDK | Много пользователей, бесплатно | Зависимость от экосистем |

Предложение: **Telegram как основной + MAX + flashcall как fallback**. VK/Яндекс — позже, если найдётся спрос.

## Уведомления — те же каналы + MAX

Сейчас: Firebase Push, Telegram, Zvonok, SMS (SMS — только как резерв auth). После фазы добавим MAX как канал получения уведомлений. Модель `notification_settings` расширится новым флагом.

## Технический блок (совмещённый с P1)

В этой же фазе делаем то, что не хотели делать точечно:

1. **`token.signing.key` вынести с диска** — в systemd env (`JASYPT_ENCRYPTOR_PASSWORD` + зашифровать обратно `application-prod.yml`) или в vault.
2. **Dual-key signing** — `primary` для выпуска, `primary + fallback` для валидации. Позволяет ротацию без массового релогина.
3. **Ротация ключа** — один раз запланированным ходом: новый ключ → старый в fallback → через X дней выключаем старый.
4. **401-handler в мобайле** — обязательно, иначе часть пользователей застрянет на пустых экранах.
5. **Refresh token mechanism** — чтобы можно было сократить срок жизни access-токена.

## Предусловия

- Phase 2 завершена (Telegram-бот стабилен и готов принимать auth-запросы).
- Мобайл умеет обрабатывать 401 (первая мини-фаза внутри BF2).

## План реализации (верхнеуровнево)

1. **Мобайл 401-handler + релиз** (без сервера).
2. **Backend: dual-key + новый endpoint `user.login_telegram`** (на базе inline-auth через бота).
3. **Мобайл: кнопка «Войти через Telegram»** → deep-link → бот подтверждает → backend выдаёт JWT.
4. **Backend: ротация скомпрометированного ключа** через dual-key.
5. **MAX login** — когда будет готов их API / токен.
6. **Уведомления через MAX** — реализация `MaxMessengerService implements Sender`.
7. **Force update старых версий** через `version_control.min` — в самом конце, когда новая версия клиента распространилась.

## Telegram Gateway API — код по номеру (near-term)

**Что:** код подтверждения для текущего телефонного входа доставляется в Telegram через официальный [Telegram Gateway API](https://core.telegram.org/gateway) (`~$0.01`/код, до 50× дешевле SMS), с фолбэком на flashcall/SMS. Идентичность остаётся = телефон, проверка кода — текущая. **Не требует** JWT-редизайна (P1) как предусловия — самый дешёвый и изолированный инкремент BF2.

### Как работает Gateway (факты из доки)

- База `https://gatewayapi.telegram.org`, авторизация `Authorization: Bearer <access_token>` (токен из аккаунта на gateway.telegram.org).
- Пользователю **не нужно запускать бота** — код приходит в системный чат «Verification Codes». Нужен лишь активный аккаунт Telegram на номере.
- Методы: `checkSendAbility(phone_number)` (бесплатно, вернёт `request_id`; платно только при успешной отправке) → `sendVerificationMessage(phone_number, code, ttl, request_id)` → (проверку кода в Режиме B делаем сами). Есть `checkVerificationStatus` и `revokeVerificationMessage` (нужны только Режиму A).
- Ответ `RequestStatus`: `request_id`, `request_cost`, `remaining_balance`, `delivery_status` (`sent/delivered/read/expired/revoked`), `is_refunded`. **Авто-возврат денег**, если код не доставлен за `ttl`.

### Режим B (выбран): Telegram — только доставка

Мы генерим и проверяем код сами (как сейчас), Telegram лишь доставляет. Минимум изменений, переиспользует `authentication_code` + `user.login_oauth_code`.

### Компоненты

- **`TelegramGatewayService`** — HTTP-клиент к Gateway: `checkSendAbility(phone)`, `sendVerificationMessage(phone, code, ttl, requestId)`. **Через тот же SOCKS5-прокси** (sing-box `127.0.0.1:10808`) — `gatewayapi.telegram.org` в РФ заблокирован, как и `api.telegram.org` (см. [TECH_DEBT P17](../TECH_DEBT.md)).
- **`TelegramGatewayProperties`** (`telegram.gateway.*`): `enabled`, `accessToken` (ENC через Jasypt в prod), `ttl`, `codeLength`. Прокси переиспользуем из `telegram.proxy.*`.
- Интеграция в `LoginAuthMobileService`/`MessageService` — единая точка выбора канала.

### Поток

1. `POST /api/user.login_oauth_mobile(phone)` → бэкенд **генерит 4-значный код и пишет в `authentication_code`** (новый шаг: раньше код возвращал Zvonok flashcall).
2. `checkSendAbility(phone)` → если доставится в Telegram → `sendVerificationMessage(phone, code=нашкод, request_id=...)`.
3. Иначе (нет Telegram / `gateway.enabled=false` / ошибка / нет баланса) → **текущий flashcall+SMS** (вход не ломается).
4. `POST /api/user.login_oauth_code(phone, code)` — **без изменений**, сверяет `authentication_code` → JWT.
5. (Опц.) ответ шага 1 отдаёт `channel: telegram|call|sms` → фронт показывает «Код отправлен в Telegram (чат “Verification Codes”)».

### Тесты

- Unit `TelegramGatewayService` (мок HTTP: ok / номер-без-Telegram / ошибка / нет баланса).
- Unit логики выбора канала: TG доставится → gateway; нет/ошибка/выкл → fallback.
- Ручная проверка: Gateway даёт бесплатный test-env и бесплатную отправку на свой номер.

### Предусловия / ops

- Регистрация на gateway.telegram.org, получение access token, пополнение баланса.
- Рабочий SOCKS5-прокси (sing-box) — уже есть на проде (P17).

### Не входит (YAGNI)

- Режим A (Telegram сам генерит/проверяет код) — больше изменений, не нужно.
- Telegram Login Widget (deep-link, вход по аккаунту), MAX, VK/Яндекс — остаются в общем плане BF2 на потом.
- JWT-редизайн (P1) — полезен, но **не** предусловие этого инкремента.

### Риски

- Зависимость от прокси: узел ляжет → Gateway недоступен → спасает SMS-фолбэк (вход продолжает работать).
- UX: код приходит в чат «Verification Codes», а не от вашего бота — обязательно пояснить на экране ввода кода.
- Баланс Gateway — мониторить `remaining_balance` из ответа; при нуле — авто-фолбэк на SMS.

## VK ID — вход по номеру (near-term, бесплатно)

**Что:** «Войти через VK» (OAuth 2.1 + PKCE) как бесплатный канал входа для РФ (~100 млн пользователей). VK ID **возвращает проверенный номер телефона** → маппим на текущих телефонных пользователей, идентичность и JWT не меняются. Дешевле Telegram Gateway (у того минимум пополнения $100). Сравнение каналов — см. историю BF2.

**Решение по идентичности (2026-06-27):** **по номеру телефона** (`findOrCreateByPhoneNumber`), без нового столбца. Требует scope `phone` (может потребовать верификации приложения в VK; если scope недоступен — запасной путь по `vk_user_id`).

**Объём шага 1: только бэкенд** (мобайл — следующим шагом).

### Поток

1. Приложение (Flutter, позже) получает VK `access_token` через `vkid_flutter_sdk`.
2. `POST /api/user.login_vk {access_token}` (whitelist, без JWT).
3. Бэкенд → VK `user_info` (`https://id.vk.ru/oauth2/user_info`, server-to-server, `access_token` + `client_id`) → проверенный телефон (+ VK user id). **Телефону с клиента не доверяем** — берём только из ответа VK.
4. Нормализуем телефон к формату `7XXXXXXXXXX` → `findOrCreateByPhoneNumberAndActivate` → JWT (как `user.login_oauth_code`).

### Компоненты (бэкенд)

- `VkIdProperties` (`vk.*`): `enabled` (default false), `clientId`, `userInfoUrl` (default `https://id.vk.ru/oauth2/user_info`).
- `VkIdService.fetchVerifiedPhone(accessToken)` → нормализованный телефон; кидает ошибку при невалидном токене / отсутствии телефона.
- DTO ответа VK (`user_id`, `phone`); `LoginVkRqParams {access_token}`; ответ — `LoginAuthCodeRsParams {token}`.
- Сервис входа: `VkIdService` → `userService.findOrCreateByPhoneNumberAndActivate` → `jwtService.generateToken`.
- Whitelist `/api/user.login_vk` в `SecurityConfiguration`.
- VK в РФ не блокируется → **прокси не нужен** (в отличие от Telegram).

### Предусловия (ops, владелец)

- Регистрация приложения в VK ID (id.vk.ru / RuStore Console): `client_id` (+ `client_secret` для мобайла), Android package + SHA-256, iOS bundle id, redirect URI.
- Доступ к scope `phone` (возможна верификация приложения).

### Тесты

- `VkIdService` (MockRestServiceServer: ok с телефоном / ok без телефона → ошибка / невалидный токен / нормализация формата).
- Сервис входа: мок VkIdService → findOrCreate → JWT.

### Не входит (шаг 1)

- Мобильная часть (`vkid_flutter_sdk` + кнопка «Войти через VK») — отдельный шаг после регистрации в VK.
- Идентичность по `vk_user_id` — только если scope `phone` окажется недоступен.

### Риски

- scope `phone` может требовать верификации приложения в VK.
- Точные имена полей в ответе `user_info` уточнить по доке VK при реализации.
- Нормализация телефона: VK может вернуть `+7…`/`7…`/маску — привести к `7XXXXXXXXXX`.

## Связанное

- Техдолг: [P1, P4, P5](../TECH_DEBT.md) (JWT/секреты), [P17](../TECH_DEBT.md) (Telegram-прокси — переиспользуется Gateway'ем).
- Текущий auth-флоу: [F1](../features/F1_AUTH.md).
- Telegram-бот (предусловие для Login Widget, но **не** для Gateway): [F8](../features/F8_TELEGRAM_BOT.md) + [BF1](BF1_TELEGRAM_BOT_UI.md).
- Telegram Gateway: <https://core.telegram.org/gateway> и <https://core.telegram.org/gateway/api>.
- Память: [project_auth_redesign_planned.md](../../.claude/projects/.../memory/project_auth_redesign_planned.md).
