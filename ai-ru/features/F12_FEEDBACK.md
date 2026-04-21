# F12: Feedback (поддержка → Telegram)

**Статус:** ✅ В проде · **Последний апдейт:** 2026-04-21

## Что делает

Форма обратной связи: пользователь (из мобильного приложения или с сайта) пишет email + текст → сохраняется в БД + пересылается в Telegram-канал поддержки. Разработчики видят обращения сразу в чате, не заходя в админку.

## Сценарии

### Из мобильного приложения

1. Авторизованный пользователь отправляет `POST /api/feedback.send { email, text }`.
2. `FeedbackFacade.send` ставит `channel = APP`, берёт `userId` из контекста.
3. `FeedbackService.create` сохраняет `Feedback` в БД.
4. `MessageService.sendMail(email, text)` — несмотря на имя метода, **отправляет в Telegram-канал** через `TelegramBotService.sendFeedback`.

### С сайта (анонимно)

1. `POST /api/feedback/send { email, text }` — анонимный endpoint (REST-style path, не `.` как в mobile).
2. `FeedbackFacade.sendAnonymous` ставит `channel = WEB`, `userId = null`.
3. Дальше тот же путь — БД + Telegram.

### Отправка в Telegram

- `TelegramBotService.sendFeedback(from, text)`:
  - формат сообщения: `<email>:\n<text>`.
  - канал = `telegram.feedbackChannelId` из `TelegramProperties`. Если `null` — feedback не шлётся, но в БД остаётся.
- Выполняется асинхронно (`pushExecutorService.execute`).

## Каналы (`FeedbackChannels`)

| Значение | Описание |
|----------|----------|
| `APP` | пришло из мобильного приложения |
| `WEB` | пришло с сайта |
| `TELEGRAM` | объявлен в enum, но **не используется** в коде |

## API / Интеграция

| Endpoint | Аутентификация | Описание |
|----------|----------------|----------|
| `POST /api/feedback.send` | JWT | Feedback из mobile |
| `POST /api/feedback/send` | нет | Feedback с веба |

## Реализация

**Backend:**
- `controller/FeedbackController` — два endpoint'а (mobile + web).
- `service/FeedbackFacade` — простая обёртка: ставит `channel`, вызывает service, дёргает `messageService.sendMail`.
- `service/FeedbackService` — `create` в БД.
- `service/message/MessageService.sendMail` — пересылает в Telegram (название метода историческое — раньше слал email).
- `service/message/telegram/TelegramBotService.sendFeedback` — отправка в `feedbackChannelId`.
- `model/Feedback` — `id`, `user_id` null-для-web, `email`, `text`, `channel`.

**Mobile:** `lib/screens/feedback_screen`, `lib/screens/support_screen`.

**Frontend:** форма отправки в одной из страниц (`index.html` / `qr.html` / `notification.html`).

## Ограничения / известный техдолг

- **`MessageService.sendMail` называется `sendMail`, но шлёт в Telegram**, а закомментированный fallback на email (`mailSender.sendMessage`) не работает. Название метода вводит в заблуждение.
- **Email нигде не валидируется** — любая строка попадает в БД и в Telegram.
- **`FeedbackChannels.TELEGRAM`** объявлен, но не используется — если Telegram-бот когда-то создавал feedback из чата (есть `FeedbackDto` / `FeedbackDtoRqWeb`), сейчас этот путь отсутствует.
- **`sendFeedbackWeb` возвращает `ResponseEntity<?>`** с `FeedbackDto` внутри, но в аннотации объявлен `FeedbackDto` как тип ответа — нетривиально для клиента.
- **Нет rate-limit** на `feedback/send` (анонимный) — можно флудить канал поддержки.
- **`MailSender`** класс существует (`service/message/mail/`), но закомментирован — мёртвый код.
- **Отсутствует способ ответить пользователю** из Telegram-канала: сообщения приходят однонаправленно.

## Ссылки

- Связанные фичи: [F8](../FEATURES.md) (Telegram-бот как транспорт).
- Код: `backend/src/main/java/ru/car/controller/FeedbackController.java`, `backend/src/main/java/ru/car/service/Feedback*.java`, `backend/src/main/java/ru/car/service/message/MessageService.java#sendMail`.
