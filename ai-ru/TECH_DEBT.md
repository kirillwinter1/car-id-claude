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
| B1 | ~~Баг на границе суток~~ **Починено 2026-04-21** — `LocalDateTime.now().minusX(...)` в `AuthenticationCodeService.isAlreadySent` и `NotificationFacade.readBy`. | 🟠 → ✅ | [F1](features/F1_AUTH.md), [F5](features/F5_NOTIFICATIONS.md) |
| B2 | ~~`qr.delete` удаляет ВСЕ уведомления юзера~~ **Починено 2026-04-21** — `notificationRepository.deleteByQrId(qr.getId())` вместо `deleteAllByUserId`. Покрыто unit-тестом в `QrServiceTest`. | 🟠 → ✅ | [F2](features/F2_QR_CODES.md) |
| B3 | **TTL временного QR: код vs документация** — код удаляет «старше 1 часа», планировщик раз в 2 часа → фактический TTL `[1h, 3h]`. Документация CLAUDE.md/RULES.md говорит 2 часа. | 🟡 | [F3](features/F3_TEMPORARY_QR.md) |
| B4 | **Rate-limit веба: код vs UI** — в коде `updateDraft`→`SEND` блокируется через 1 минуту (`findByQrIdAndDateAfter(period=1min)`), на `notification.html` фронт пишет «1 раз в час». Либо код, либо текст неправильны. | 🟡 | [F4](features/F4_WEB_REPORT.md) |
| B5 | **`isValid(visitorId)` не вызывается** — написан в `NotificationService`, но не применяется; любая строка пишется в БД. | 🟡 | [F4](features/F4_WEB_REPORT.md) |
| B6 | **Нет фонового уборщика DRAFT-уведомлений** — удаляются «лениво» только при попытке `findById`; если никто не читает — висят в БД. | 🟡 | [F4](features/F4_WEB_REPORT.md) |
| B7 | ~~Пустые catch-блоки~~ **Починено 2026-04-21** — `TelegramLogicService` и `FirebaseService` теперь логируют исключения через `log.warn(...)`. | 🟠 → ✅ | [F7](features/F7_FIREBASE_PUSH.md), [F8](features/F8_TELEGRAM_BOT.md) |
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
| D5 | ~~`FirebaseService.main` — тестовый метод с реальным токеном~~ **Удалён 2026-04-21**. | 🟠 → ✅ | [F7](features/F7_FIREBASE_PUSH.md) |
| D6 | `SmsAeroClient` — 270 строк скопированного клиента, большая часть методов (Viber, ContactList, HlrCheck, GroupAdd и др.) не используется. | 🟡 | [F10](features/F10_SMS.md) |
| D7 | ~~Закомментированные блоки в `MessageService`~~ **Удалены 2026-04-21**. | ⚪ → ✅ | — |

---

## Architecture

| # | Проблема | Приоритет | Ссылка |
|---|----------|-----------|--------|
| A1 | ~~**Telegram: циклическая зависимость + `@Setter` на классе** — `TelegramBotService ↔ TelegramLogicService` разрывается через setter-инъекцию в `TelegramConfig`; `@Setter` на уровне класса делает все поля мутируемыми.~~ **Закрыто 2026-04-21 в Phase 2.1** — выделен `TelegramTransport`-интерфейс, `TelegramRouter` и сцены зависят от него; `@Setter` убран. | 🟠 → ✅ | [F8](features/F8_TELEGRAM_BOT.md) |
| A2 | ~~**Telegram: монолитный `onUpdateReceived`** — авторизация + callback-роутинг + switch по текстовым командам в одной функции; новая команда = +case. Сложно тестировать.~~ **Закрыто 2026-04-21 в Phase 2.1** — разбит на `TelegramRouter`, `TelegramAuthorizationService`, и сцены (`HomeMenuScene`, `QrListScene`, `TemporaryQrScene`, `NotificationMarkReadScene`). | 🟠 → ✅ | [F8](features/F8_TELEGRAM_BOT.md) |
| A3 | ~~**Telegram: `TelegramBotService` ходит в `NotificationRepository`** — нарушение слоя. Бот должен быть тонким транспортом.~~ **Закрыто 2026-04-21 в Phase 2.1** — бот больше не импортирует `NotificationRepository`; `NotificationMarkReadScene` использует `NotificationService.shouldShowMarkAsReadButton` и `NotificationFacade.readBy`. | 🟡 → ✅ | [F8](features/F8_TELEGRAM_BOT.md) |
| A4 | ~~**Magic strings в Telegram** — `"Неизвестная команда"`, `"QR-коды:"`, `"отметить прочитанным"` и т.п. разбросаны по коду, без централизованных констант/i18n.~~ **Закрыто 2026-04-21 в Phase 2.1** — все тексты вынесены в `resources/i18n/telegram_ru.properties` через Spring `MessageSource` + `TelegramMessages` wrapper. | 🟡 → ✅ | [F8](features/F8_TELEGRAM_BOT.md) |
| A5 | **JdbcTemplate везде** — много boilerplate, ручной маппинг, нет типобезопасности. Для сложных запросов JdbcTemplate оправдан, для CRUD — лучше Spring Data JPA. | 🟡 | — |
| A6 | **POST для всех endpoints** — не-RESTful, нет HTTP-кэширования. | 🟡 | — |
| A7 | **Нет API versioning** — нет `/api/v1/...`. Breaking changes сломают клиентов. | 🟡 | — |
| A8 | **Нет OpenAPI спецификации** (SpringDoc подключён, но не проверялся) — документация устаревает, интеграция руками. | 🟡 | — |
| A9 | **`batchId = 1L` захардкожен** в `QrService.createQr` / `createTemporaryQr` — механика назначения batch'ей не используется. | 🟡 | [F2](features/F2_QR_CODES.md) |
| A10 | **`printed` флаг у QR не используется** (только хранится в БД). | ⚪ | [F2](features/F2_QR_CODES.md) |
| A11 | **Нет permission-check на `qr.create`** — любой авторизованный может создать QR, хотя предполагается админ-онли. | 🟡 | [F2](features/F2_QR_CODES.md) |
| A12 | ~~`SmsService.canSendNotification` возвращает `true` всегда~~ **Починено 2026-04-21** — теперь `false` с комментарием, защита от случайного включения SMS в веер рассылки. Когда SMS станет реальным каналом уведомлений — заменим на `setting.getSmsEnabled()`. | 🟡 → ✅ | [F10](features/F10_SMS.md) |
| A13 | **`ReasonDictionary` одномерный** — нет групп/категорий/иконок/i18n; нет soft-delete/`active`; порядок задаётся только `id`. | 🟡 | [F13](features/F13_REASONS.md) |
| A14 | **`Marketplaces` и `VersionControl` — singleton-row без enforcement** — нет unique/check на «ровно одна строка», `findFirst` берёт «первую попавшуюся»; правильнее держать в конфиге/админке. | 🟡 | [F14](features/F14_MARKETPLACES.md), [F15](features/F15_VERSION_CONTROL.md) |
| A15 | **`Zvonok` сервис — три разных API (`sendCode` / `sendCodeMessage` / `sendMessage`) в одном классе** — разные возвращаемые типы и семантика ошибок. | 🟡 | [F11](features/F11_ZVONOK.md) |
| A16 | **`UserDtoMapper` ссылается на `telegramChatId`** (unmapped), хотя такого поля в `User` нет — артефакт старой модели. | ⚪ | — |
| A17 | **`RestTemplate` пересоздаётся в `FirebaseService.send` на каждый вызов** — без пула и таймаутов. | 🟡 | [F7](features/F7_FIREBASE_PUSH.md) |
| A18 | **Нет ретраев / обработки статусов в `WhatsappBotService`-аналогах** — для любого канала доставки любой сбой = silent fail. | 🟡 | — |
| A19 | **`TelegramBotService` совмещает `Sender` и `TelegramTransport`** — создаёт самоссылочную DI-зависимость, потребовавшую 3 `@Lazy`-инъекции на сценах/рендерере. Архитектурный smell; кандидат на расщепление (новый `TelegramDispatcher` как отдельный бин-transport, `TelegramBotService` только как entrypoint). | 🟡 | [F8](features/F8_TELEGRAM_BOT.md) |

---

## Tests

| # | Проблема | Приоритет | Ссылка |
|---|----------|-----------|--------|
| T1 | **`CarIdApplicationTests.java`** — `@SpringBootTest` и `@Test` **закомментированы** — не стартует. | 🔴 | — |
| T2 | **`IntegrationTest.java`** (Selenide E2E) — `@Test` **закомментирован**, использует внешний URL. | 🟠 | — |
| T3 | **Baseline unit-тесты на ядро добавлены 2026-04-21** — `JwtServiceTest`, `NotificationServiceTest`, `QrServiceTest`, `QrRepositoryIntegrationTest` (~2000 LoC, 64 теста). Не покрыто: `MessageService`, `AuthenticationCodeService`, `NotificationSettingService`, `UserService`, каналы доставки — наращиваем по мере работы с этими сервисами. | 🔴 → 🟡 | — |
| T4 | **Mobile тесты = 0%** — `widget_test.dart` это шаблонный counter-test. | 🟠 | — |
| T5 | **Frontend тесты отсутствуют.** | 🟡 | — |
| T6 | ~~Нет CI-гейта на тесты~~ **Починено 2026-04-21** — `.github/workflows/backend-test.yml` запускает `./gradlew test` на каждый PR и push в main; красный билд блокирует merge. | 🔴 → ✅ | — |

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

## Production / Operations

> Находки при прямом обзоре prod-машины `79.174.94.70` **2026-04-21**. Конкретные значения — в приватном `ops.inf`.

| # | Проблема | Приоритет | Ссылка |
|---|----------|-----------|--------|
| P1 | **Все секреты лежат plain text в `/var/www/car_id/application-prod.yml`** на prod-сервере (`token.signing.key` по умолчанию из `${TOKEN_SIGNING_KEY:…}`, PG password, Telegram token, SMS Aero apiKey, admin.code, mail password). Хотя в git репо они через Jasypt `ENC(...)` — **на проде распакованы**. Любой с root-доступом читает всё; JWT можно штамповать произвольно. Ротация + восстановление Jasypt-шифрования на диске. | 🔴 | [OPERATIONS.md](OPERATIONS.md) |
| P2 | **Backup был сломан 47 дней** (5 марта — 21 апреля 2026). `backup.sh` ожидал интерактивный ввод пароля, падал с `fe_sendauth: no password supplied`. **Починено 2026-04-21**: добавлен `/root/.pgpass`, ротация переписана на `find -size +0`, 0-байтные файлы вычищены, локальная копия на MacBook через `~/bin/pull-car-id-backups.sh` + launchd. Оригинальный скрипт на проде не в git — закрывается в P3. | 🔴 → ✅ | [OPERATIONS.md](OPERATIONS.md) |
| P3 | **Deploy-скрипты + systemd unit-ы жили только на проде, не в git** — `backup.sh`, `run.sh`, `health.sh`, `send_message.sh`, `*.service`, `*.timer`. При пересоздании машины были бы потеряны. **Заведено в `deploy/`** с env-параметризацией для публичного репо. Требуется миграция prod на env-driven версию. | 🟠 → 🟡 | [`../deploy/`](../deploy/) |
| P4 | **`send_message.sh` содержит Telegram bot token в clear-text** на сервере + chat_id админ-канала. На проде-скрипт — хардкод; в `deploy/send_message.sh` перенесено на env vars. Требуется ротация токена (он уже скомпрометирован — я прочитал). | 🟠 | [`../deploy/send_message.sh`](../deploy/send_message.sh) |
| P5 | **`/actuator/health` каждые 5 сек дёргает healthcheck** через `JwtAuthenticationFilter`, который пишет **DEBUG `NotAuthorized ... sessionId = ...`** в лог. В сочетании с `logger.levels.ru.car: DEBUG` на проде — журналы забиваются session-id'ами (утечка). Поднять до INFO, вынести actuator из-под JWT-фильтра. | 🟠 | — |
| P6 | **`logger.levels.ru.car: DEBUG` на проде.** + synonym: `/actuator/health` publicly accessible из интернета через nginx. Логи содержат debug-сведения. Перевести на INFO. | 🟠 | — |
| P7 | **Swagger UI открыт публично** (`https://car-id.ru/swagger-ui.html`, `springdoc.enabled: true`). Утечка API surface для атакующего. Выключить на prod (`springdoc.enabled: false`) или закрыть basic-auth в nginx. | 🟠 | — |
| P8 | **`management.endpoints.web.exposure.include: health,prometheus`** — endpoint'ы без аутентификации. `/actuator/health` содержит `show-details: always` — детали компонентов тоже на публике. `/actuator/prometheus` — метрики без auth. | 🟠 | [TECH_DEBT S8, O5](#) |
| P9 | **nginx разрешает TLSv1 + TLSv1.1** (`ssl_protocols TLSv1 TLSv1.1 TLSv1.2`). Устаревшие версии уязвимы. Оставить только TLSv1.2 + TLSv1.3. Плюс там же длиннющий список cipher'ов — пора на современный set. | 🟡 | — |
| P10 | **Backend слушает `*:8081`** (все интерфейсы). Публикация наружу через nginx на 443, но если у провайдера нет firewall — порт достижим в обход nginx без SSL/rate-limit. Перейти на `127.0.0.1:8081` или настроить firewall. | 🟠 | — |
| P11 | **`car_id.jar` собран 4 ноября 2025**, миграции 1.9–1.17 и наши доработки туда не попали. 5+ месяцев без деплоев. Liquibase в prod БД тоже отстаёт. | 🟡 | — |
| P12 | **Java runtime на проде = OpenJDK 21, в `build.gradle` = `sourceCompatibility '17'`.** Несогласованность. Либо поднять source/target до 21, либо запускать prod на 17. | 🟡 | — |
| P13 | **`run.sh` хардкодит `/usr/lib/jvm/jdk-21.0.3/bin/java`** — при минорном обновлении Java версия пропадёт. В `deploy/run.sh` уже через `CAR_ID_JAVA_BIN`. | ⚪ | [`../deploy/run.sh`](../deploy/run.sh) |
| P14 | **Диск `/` на проде 81% (16/20 GB)**, RAM 86 MB free, swap активен 492 MB. На машине ещё живут Lead Board + CRM + Prometheus/Grafana/Alertmanager. Нужно либо почистить (старые backup'ы Lead Board, логи Docker), либо апгрейдить VM. | 🟠 | — |
| P15 | **PostgreSQL — удалённый сервер** `79.174.88.176:15965`. Нет health-check'а со стороны приложения; нет connection pool мониторинга в prometheus. | 🟡 | — |
| P16 | **`health.service` имеет `Description=Backup`** (скопипащено с `backup.service`). В `deploy/systemd/health.service` исправлено на «Car-ID healthcheck». | ⚪ | [`../deploy/systemd/health.service`](../deploy/systemd/health.service) |

---

## Приоритетный план (roadmap)

### Фаза «Prod safety» (срочно, в первую очередь)

1. **P1** Ротировать все скомпрометированные секреты (JWT signing key, PG password, Telegram token, SMS Aero key, admin.code, mail password) + вернуть Jasypt-шифрование `application-prod.yml` на диске.
2. **P4** Ротация Telegram alert-token (из `send_message.sh`) и перевод на env vars из `deploy/`.
3. **P7** Выключить Swagger UI на prod (`springdoc.enabled: false` в prod-конфиге).
4. **P8** Ограничить `/actuator/*` (basic-auth через nginx или `include: health` + auth).
5. **P14** Освободить диск / спланировать апгрейд VM (81% заполнения критично).

### Фаза «Safety net» (perm-quality, must-do)

6. **S1** JWT expiration + refresh token.
7. **T1–T3, T6** — раскомментировать тесты, довести unit-тесты на ядро, CI-гейт.
8. **S5** — вынести SMS Aero credentials в конфиг с Jasypt, ротировать.
9. **S2** — CORS whitelist.
10. **S4** — rate limiting на login endpoints.
11. **S7** — защитить Zvonok callbacks (подпись / HMAC).
12. **P6** Снизить `logger.levels.ru.car` до INFO на проде.
13. **P10** Ограничить backend до `127.0.0.1:8081` или firewall-правило.
14. **P9** Отключить TLSv1/TLSv1.1 в nginx.

### Фаза «Bug bash»

7. **B1** — починить границу суток (переход на `LocalDateTime.now().minusX(...)`).
8. **B2** — исправить каскадное удаление в `qr.delete`.
9. **B3–B4** — привести TTL/rate-limit: код ↔ доки.
10. **B7** — убрать пустые catch.
11. **B12** — чистить мёртвые Firebase-токены по ответу FCM.

### Фаза «Архитектурный рефакторинг Telegram-бота» (A1–A4)

Закрывается в рамках **Phase 2 эпика Telegram-бот** (2026-04-21). См.:
- Мастер-документ: [review/2026-04-21_TELEGRAM_EPIC.md](review/2026-04-21_TELEGRAM_EPIC.md).
- Под-спек 2.1: [review/2026-04-21_TG_2.1_ARCHITECTURE.md](review/2026-04-21_TG_2.1_ARCHITECTURE.md) — закрывает A1–A4 preserve-behaviour рефакторингом.

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
