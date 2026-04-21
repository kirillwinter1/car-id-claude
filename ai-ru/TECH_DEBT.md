# Технический долг Car-ID

> Единый реестр техдолга. Группировка по категориям. Для фичевого техдолга — ссылка на карточку в [features/](features/), где есть полный контекст. Сквозной техдолг (security, архитектура, тесты, devops) — раскрыт здесь.
>
> Помечаем приоритет:
> - 🔴 **Critical** — security/data loss/регрессии в проде
> - 🟠 **High** — заметный баг или блокер для развития
> - 🟡 **Medium** — улучшает качество, но не горит
> - ⚪ **Low** — nice-to-have

---

## Security

| # | Проблема | Приоритет | Ссылка |
|---|----------|-----------|--------|
| S1 | **JWT без expiration** — `setExpiration` закомментирован в `JwtService.generateToken`, `isTokenValid` не проверяет срок. Украденный токен работает вечно. | 🔴 | [F1](features/F1_AUTH.md) |
| S2 | **CORS `*`** — `SecurityConfiguration.allowedOrigins("*")`. XSS с любого домена. | 🔴 | — |
| S3 | **Admin hardcoded в конфиге** — `admin.phone` + `admin.code` в `application-dev.yml` (`code: 1111`); позволяет обходить flashcall. | 🔴 | [F1](features/F1_AUTH.md) |
| S4 | **Нет rate limiting на login endpoints** — можно brute-force код + флудить flashcall. | 🔴 | [F1](features/F1_AUTH.md) |
| S5 | **SMS Aero credentials в коде** — `email`, `apiKey`, `sign` захардкожены как `static final` в `SmsService.java`. Секрет в репозитории. | 🔴 | [F10](features/F10_SMS.md) |
| S6 | **`FirebaseService.main()`** — ручной тест с реальным FCM-токеном и путём к service account JSON. Остаток отладки в prod-классе. | 🟠 | [F7](features/F7_FIREBASE_PUSH.md) |
| S7 | **Zvonok callback-endpoints не аутентифицированы** — `GET /api/zvonok/*` принимают `callId`, `phoneFrom`, `campaignId` без подписи. Возможна подделка `markAsRead` и впрыск авторизационных кодов. | 🔴 | [F11](features/F11_ZVONOK.md) |
| S8 | **`/actuator/health` — `show-details: always`** — детали компонентов видны анонимно. | 🟡 | [F16](features/F16_METRICS_MONITORING.md) |
| S9 | **Логирование sensitive данных** — `sendSmsCode` пишет телефон+код в `log.info`; в других местах телефоны в `log.debug`. | 🟠 | [F10](features/F10_SMS.md) |
| S10 | **HTTP для внешних сервисов** — не все URL форсируют HTTPS. | 🟠 | — |
| S11 | **Нет CSP headers** в nginx/фронте — XSS. | 🟡 | — |
| S12 | **Публичные endpoints без `/visitor_id` rate-limit** — `/api/report/createDraft` и `/api/feedback/send` без защиты. | 🟠 | [F4](features/F4_WEB_REPORT.md), [F12](features/F12_FEEDBACK.md) |

---

## Bugs

| # | Проблема | Приоритет | Ссылка |
|---|----------|-----------|--------|
| B1 | **Баг на границе суток** — `LocalDateTime.of(LocalDate.now(), LocalTime.now().minusMinutes(60))` в `NotificationFacade.readBy` и `LocalTime.now().minusSeconds(..)` в `AuthenticationCodeService.isAlreadySent` дают отрицательное время около 00:00. | 🟠 | [F1](features/F1_AUTH.md), [F5](features/F5_NOTIFICATIONS.md) |
| B2 | **`qr.delete` удаляет ВСЕ уведомления юзера по userId** — должно быть по qrId. | 🟠 | [F2](features/F2_QR_CODES.md) |
| B3 | **TTL временного QR: код vs документация** — код удаляет «старше 1 часа», планировщик раз в 2 часа → фактический TTL `[1h, 3h]`. Документация CLAUDE.md/RULES.md говорит 2 часа. | 🟡 | [F3](features/F3_TEMPORARY_QR.md) |
| B4 | **Rate-limit веба: код vs UI** — в коде `updateDraft`→`SEND` блокируется через 1 минуту (`findByQrIdAndDateAfter(period=1min)`), на `notification.html` фронт пишет «1 раз в час». Либо код, либо текст неправильны. | 🟡 | [F4](features/F4_WEB_REPORT.md) |
| B5 | **`isValid(visitorId)` не вызывается** — написан в `NotificationService`, но не применяется; любая строка пишется в БД. | 🟡 | [F4](features/F4_WEB_REPORT.md) |
| B6 | **Нет фонового уборщика DRAFT-уведомлений** — удаляются «лениво» только при попытке `findById`; если никто не читает — висят в БД. | 🟡 | [F4](features/F4_WEB_REPORT.md) |
| B7 | **Пустые catch-блоки** — `catch (Exception ignore) {}` в `TelegramLogicService` при read-callback; `catch (Exception ignored) {}` в `FirebaseService.sendNotification` per-token. Ошибки глушатся без лога. | 🟠 | [F7](features/F7_FIREBASE_PUSH.md), [F8](features/F8_TELEGRAM_BOT.md) |
| B8 | **Пагинация без явной сортировки** — `PageParam` не содержит `orderBy`; порядок определяется реализацией репозитория, возможны пропуски/дубли на границе страниц. | 🟡 | [F5](features/F5_NOTIFICATIONS.md) |
| B9 | **Проверка баланса SMS Aero через `.equals(0.00)`** — не ловит 0.01; + `System.out.println("Insufficient balance")` вместо логгера. | 🟡 | [F10](features/F10_SMS.md) |
| B10 | **`destroyAllTemporaryQr` использует жёсткое `destroy`** — удаление из БД, не soft-delete; ломает аудит. | 🟡 | [F3](features/F3_TEMPORARY_QR.md) |
| B11 | **`createTemporaryQr` блокируется даже удалёнными QR** — предусловие «нет ни одного QR» без фильтра по статусу. | 🟡 | [F3](features/F3_TEMPORARY_QR.md) |
| B12 | **Нет инвалидации мёртвых Firebase-токенов** — FCM возвращает `UNREGISTERED`, код не чистит. Таблица растёт. | 🟡 | [F7](features/F7_FIREBASE_PUSH.md) |

---

## Dead code

| # | Проблема | Приоритет | Ссылка |
|---|----------|-----------|--------|
| D1 | `MailSender` и импорты email-отправки — никогда не используется в рассылке; `MessageService.sendMail` фактически шлёт в Telegram. | 🟡 | [F12](features/F12_FEEDBACK.md) |
| D2 | `MonitoringBeanPostProcessor` закомментирован (`//@Component`); аннотация `@Monitoring` существует, но не применяется. Решение: активировать BPP или удалить вместе с аннотацией. | 🟡 | [F16](features/F16_METRICS_MONITORING.md) |
| D3 | `FeedbackChannels.TELEGRAM` — объявлен, не используется (раньше, видимо, feedback принимался из бота). | ⚪ | [F12](features/F12_FEEDBACK.md) |
| D4 | `/api/report/send` (web без черновика) — дублирует flow `createDraft → updateDraft`. Фронт не использует. | ⚪ | [F4](features/F4_WEB_REPORT.md) |
| D5 | `FirebaseService.main` — тестовый метод с реальным токеном. Удалить. | 🟠 | [F7](features/F7_FIREBASE_PUSH.md) |
| D6 | `SmsAeroClient` — 270 строк скопированного клиента, большая часть методов (Viber, ContactList, HlrCheck, GroupAdd и др.) не используется. | 🟡 | [F10](features/F10_SMS.md) |
| D7 | Закомментированные блоки в `MessageService.sendMail` (email fallback) и `sendFlashcallCode` (резерв для WhatsApp — удалён как класс). | ⚪ | — |

---

## Architecture

| # | Проблема | Приоритет | Ссылка |
|---|----------|-----------|--------|
| A1 | **Telegram: циклическая зависимость + `@Setter` на классе** — `TelegramBotService ↔ TelegramLogicService` разрывается через setter-инъекцию в `TelegramConfig`; `@Setter` на уровне класса делает все поля мутируемыми. | 🟠 | [F8](features/F8_TELEGRAM_BOT.md) |
| A2 | **Telegram: монолитный `onUpdateReceived`** — авторизация + callback-роутинг + switch по текстовым командам в одной функции; новая команда = +case. Сложно тестировать. | 🟠 | [F8](features/F8_TELEGRAM_BOT.md) |
| A3 | **Telegram: `TelegramBotService` ходит в `NotificationRepository`** — нарушение слоя. Бот должен быть тонким транспортом. | 🟡 | [F8](features/F8_TELEGRAM_BOT.md) |
| A4 | **Magic strings в Telegram** — `"Неизвестная команда"`, `"QR-коды:"`, `"отметить прочитанным"` и т.п. разбросаны по коду, без централизованных констант/i18n. | 🟡 | [F8](features/F8_TELEGRAM_BOT.md) |
| A5 | **JdbcTemplate везде** — много boilerplate, ручной маппинг, нет типобезопасности. Для сложных запросов JdbcTemplate оправдан, для CRUD — лучше Spring Data JPA. | 🟡 | — |
| A6 | **POST для всех endpoints** — не-RESTful, нет HTTP-кэширования. | 🟡 | — |
| A7 | **Нет API versioning** — нет `/api/v1/...`. Breaking changes сломают клиентов. | 🟡 | — |
| A8 | **Нет OpenAPI спецификации** (SpringDoc подключён, но не проверялся) — документация устаревает, интеграция руками. | 🟡 | — |
| A9 | **`batchId = 1L` захардкожен** в `QrService.createQr` / `createTemporaryQr` — механика назначения batch'ей не используется. | 🟡 | [F2](features/F2_QR_CODES.md) |
| A10 | **`printed` флаг у QR не используется** (только хранится в БД). | ⚪ | [F2](features/F2_QR_CODES.md) |
| A11 | **Нет permission-check на `qr.create`** — любой авторизованный может создать QR, хотя предполагается админ-онли. | 🟡 | [F2](features/F2_QR_CODES.md) |
| A12 | **`SmsService.canSendNotification` возвращает `true` всегда** — нарушение контракта `Sender`. | 🟡 | [F10](features/F10_SMS.md) |
| A13 | **`ReasonDictionary` одномерный** — нет групп/категорий/иконок/i18n; нет soft-delete/`active`; порядок задаётся только `id`. | 🟡 | [F13](features/F13_REASONS.md) |
| A14 | **`Marketplaces` и `VersionControl` — singleton-row без enforcement** — нет unique/check на «ровно одна строка», `findFirst` берёт «первую попавшуюся»; правильнее держать в конфиге/админке. | 🟡 | [F14](features/F14_MARKETPLACES.md), [F15](features/F15_VERSION_CONTROL.md) |
| A15 | **`Zvonok` сервис — три разных API (`sendCode` / `sendCodeMessage` / `sendMessage`) в одном классе** — разные возвращаемые типы и семантика ошибок. | 🟡 | [F11](features/F11_ZVONOK.md) |
| A16 | **`UserDtoMapper` ссылается на `telegramChatId`** (unmapped), хотя такого поля в `User` нет — артефакт старой модели. | ⚪ | — |
| A17 | **`RestTemplate` пересоздаётся в `FirebaseService.send` на каждый вызов** — без пула и таймаутов. | 🟡 | [F7](features/F7_FIREBASE_PUSH.md) |
| A18 | **Нет ретраев / обработки статусов в `WhatsappBotService`-аналогах** — для любого канала доставки любой сбой = silent fail. | 🟡 | — |

---

## Tests

| # | Проблема | Приоритет | Ссылка |
|---|----------|-----------|--------|
| T1 | **`CarIdApplicationTests.java`** — `@SpringBootTest` и `@Test` **закомментированы** — не стартует. | 🔴 | — |
| T2 | **`IntegrationTest.java`** (Selenide E2E) — `@Test` **закомментирован**, использует внешний URL. | 🟠 | — |
| T3 | **Нет unit-тестов на ядро** — `JwtService`, `NotificationService`, `QrService`, `MessageService`, `AuthenticationCodeService`, репозитории. *(Появились заготовки в `backend/src/test/java/ru/car/service/`, `repository/`, `test/` — нужно довести.)* | 🔴 | — |
| T4 | **Mobile тесты = 0%** — `widget_test.dart` это шаблонный counter-test. | 🟠 | — |
| T5 | **Frontend тесты отсутствуют.** | 🟡 | — |
| T6 | **Нет CI-гейта на тесты** — сборка пройдёт даже если тесты падают. | 🔴 | — |

---

## DevOps

| # | Проблема | Приоритет | Ссылка |
|---|----------|-----------|--------|
| O1 | **Нет CI для frontend** — Backend CI есть, frontend разворачивается вручную через `scp + tar`. | 🟡 | см. `ops.inf` |
| O2 | **Нет Docker** — «works on my machine», сложный деплой. | 🟡 | — |
| O3 | **Нет health-checks для внешних интеграций** — непонятно, когда падает Telegram/SMS/Zvonok. | 🟠 | — |
| O4 | **Логи в stdout** — нет structured logging (JSON/ELK/Loki); сложно искать. | 🟡 | — |
| O5 | **`/actuator/prometheus` и `/metrics` — без auth** — метрики утекают. | 🟡 | [F16](features/F16_METRICS_MONITORING.md) |
| O6 | **Нет alerting** — метрики есть, но алертов на падения нет. | 🟡 | — |
| O7 | **Postman-коллекция не в репозитории** — только «по запросу». | ⚪ | — |
| O8 | **Ручная генерация QR через `main()`** в `SvgUtils` / `PdfConverter` с захардкоженным абсолютным путём. | 🟡 | см. [OPERATIONS.md](OPERATIONS.md) |

---

## Mobile

| # | Проблема | Приоритет | Ссылка |
|---|----------|-----------|--------|
| M1 | **Нет offline queue** — действия при отсутствии сети теряются. | 🟡 | — |
| M2 | **Только русский язык** (`i18n/lang/ru_RU.dart`) — нет en. | ⚪ | — |
| M3 | **Firebase обязателен** — Huawei без GMS не получит push; HMS Push Kit не настроен. | 🟡 | [F7](features/F7_FIREBASE_PUSH.md) |
| M4 | **Crashlytics подключён, но не настроен полностью** — нет breadcrumbs, кастомных event'ов. | 🟡 | — |

---

## Frontend

| # | Проблема | Приоритет | Ссылка |
|---|----------|-----------|--------|
| W1 | **Polling статуса каждые 5 сек** — нагрузка на сервер при росте; стоит WebSocket или SSE. | 🟡 | [F5](features/F5_NOTIFICATIONS.md) |
| W2 | **Нет минификации JS/CSS** — больший размер, нет build-шага. | ⚪ | — |
| W3 | **`:has()` селектор** не работает в Safari <15.4 — фоллбэк отсутствует. | ⚪ | — |

---

## Observability

| # | Проблема | Приоритет | Ссылка |
|---|----------|-----------|--------|
| V1 | **Нет метрик по каналам доставки** — `firebase`, `telegram`, `zvonok` не инкрементят счётчики success/failure. | 🟡 | [F16](features/F16_METRICS_MONITORING.md) |
| V2 | **Нет tracing** (OpenTelemetry, Sleuth) — при падении отследить цепочку нельзя. | 🟡 | — |
| V3 | **Дублирование «метрики + БД»** — при каждом событии два вызова (`Counter.increment` + `monitoring_repository.save`). При росте таблицы `monitoring` нужна стратегия TTL/архивации. | 🟡 | [F16](features/F16_METRICS_MONITORING.md) |
| V4 | **Нет индекса `(event, created_date)`** в таблице `monitoring` — запросы «счётчик за сегодня» станут медленнее по мере роста. | 🟡 | [F16](features/F16_METRICS_MONITORING.md) |

---

## Приоритетный план (roadmap)

### Фаза «Safety net» (perm-quality, must-do)

1. **S1** JWT expiration + refresh token.
2. **T1–T3, T6** — раскомментировать тесты, довести unit-тесты на ядро, CI-гейт.
3. **S5** — вынести SMS Aero credentials в конфиг с Jasypt, ротировать.
4. **S2** — CORS whitelist.
5. **S4** — rate limiting на login endpoints.
6. **S7** — защитить Zvonok callbacks (подпись / HMAC).

### Фаза «Bug bash»

7. **B1** — починить границу суток (переход на `LocalDateTime.now().minusX(...)`).
8. **B2** — исправить каскадное удаление в `qr.delete`.
9. **B3–B4** — привести TTL/rate-limit: код ↔ доки.
10. **B7** — убрать пустые catch.
11. **B12** — чистить мёртвые Firebase-токены по ответу FCM.

### Фаза «Архитектурный рефакторинг Telegram-бота» (A1–A4)

См. будущий документ `review/2026-04-XX_TELEGRAM_REFACTORING.md`.

### Фаза «DevOps»

12. **O2** Dockerfile + docker-compose.
13. **O1** CI для frontend.
14. **O3** Health-checks каналов.
15. **O4** Structured logging.

### Остальное — по мере необходимости

Architecture/Mobile/Frontend/Observability пункты — не блокируют работу; добавляются при next-фичах.

---

## Итоговая оценка (из бывшего TECH_REVIEW.md)

| Аспект | Оценка | Комментарий |
|--------|--------|-------------|
| **Функциональность** | 8/10 | Мультиканальная доставка работает. |
| **Security** | 4/10 | JWT без expiration, CORS `*`, нет rate limit, секреты в коде. |
| **Тесты** | 2/10 | Появились заготовки, но фактически близко к 0%. |
| **Масштабируемость** | 6/10 | Монолит, OK для текущей нагрузки. |
| **Maintainability** | 6/10 | Понятная структура, но без тестов рефакторить страшно. |
| **DevOps** | 3/10 | Backend CI есть, остальное вручную. |

**Общая оценка: ~5/10.** Рабочий MVP с чёткой бизнес-логикой, но надо закрыть security и довести тесты до того, как делать следующие фичи — см. [README.md](README.md) «текущая фаза — стабилизация».
