# F16: Metrics & Monitoring

**Статус:** ✅ В проде · **Последний апдейт:** 2026-04-21

## Что делает

Две параллельные системы «учёта событий»:

1. **Prometheus-метрики через Micrometer** (`MetricService`) — счётчики `car.register_user`, `car.activate_qr`, `car.send_notification`, `car.read_notification`. Экспортируются через Actuator `/actuator/prometheus`, скрейпаются Prometheus'ом и рисуются в Grafana.
2. **БД-аудит событий** (`MonitoringService`) — те же события пишутся в таблицу `monitoring` с `user_id` и датой. Используется для дашборд-команд в Telegram-боте («сколько зарегистрировалось сегодня», см. [F8](../FEATURES.md)).

Дублирование намеренное: Prometheus хорош для временных рядов и алертов, `monitoring` таблица — для join с пользователями и per-user аналитики (из неё можно построить cohort'ы, которые Prometheus не видит).

## События (`monitoring.Events`)

| Событие | Где вызывается |
|---------|----------------|
| `REGISTER_USER` | `MetricService.register` в `UserService.findOrCreateByPhoneNumberAndActivate` |
| `ACTIVATE_QR` | `MetricService.activateQr` в `QrService.linkToUser` (только при переходе из `NEW` → `ACTIVE`) |
| `SEND_NOTIFICATION` | `MetricService.sendNotification` в `MessageService.sendPush` |
| `READ_NOTIFICATION` | `MetricService.readNotification` в `NotificationService.read` |

## Сценарии

### Инкремент метрики + запись события

1. Бизнес-код вызывает `metricService.register()` / `.sendNotification()` / `.readNotification()` / `.activateQr()`.
2. `MetricService` инкрементит Micrometer Counter.
3. `MonitoringService.monitor(event)` сохраняет строку в таблицу `monitoring` с текущим `userId` (или null).

### Telegram-админ дашборд

1. Admin в Telegram нажимает `/registerUserCount` (или другую) ([F8](../FEATURES.md)).
2. `TelegramLogicService.messageForCommand` → `MonitoringService.getRegisterCountToday()` → `monitoringRepository.findCountByEventAndAfter(event, LocalDate.now())`.
3. Бот шлёт ответ: «Сегодня зарегистрированно N пользователей».

### Prometheus scrape

1. Prometheus опрашивает `GET /actuator/prometheus` каждые 15 сек (по умолчанию).
2. Экспортируются все Micrometer метрики + счётчики `car.*`.

### Health check

1. `GET /actuator/health` — `status: UP` / `DOWN`, полный details (`show-details: always`).

## API / Интеграция

| Endpoint | Описание |
|----------|----------|
| `GET /actuator/health` | Health check + детали |
| `GET /actuator/prometheus` | Метрики в Prometheus-формате |
| `GET /actuator/metrics` | Metrics API (JSON) |

**Spring Boot Actuator** (`spring-boot-starter-actuator`) + `micrometer-registry-prometheus`.

## Реализация

**Backend:**
- `service/MetricService` — `@PostConstruct` регистрирует 4 `Counter` в `MeterRegistry`; публичные методы инкрементят + зовут `MonitoringService.monitor`.
- `monitoring/MonitoringService` — пишет в БД (`monitoring_repository`), предоставляет `getXxxCountToday` для Telegram.
- `monitoring/MonitoringRepository` — JdbcTemplate (`save`, `findCountByEventAndAfter`).
- `monitoring/Events` — enum из 4 событий.
- `monitoring/@Monitoring` — аннотация на метод «оберни в `around`».
- `monitoring/MonitoringBeanPostProcessor` — **отключён** (`@Component` закомментирован). CGLIB-прокси, который при наличии `@Monitoring` на методе автоматически пишет события. Не используется, но код есть.

**БД (таблица `monitoring`):**
- `id`, `event` (enum via string), `user_id` null-able, `created_date`.
- Нет индексов по `event + created_date` (может стать bottleneck при миллионах строк).

**Конфигурация (`application.yml`):**
- `management.endpoints.web.exposure.include: prometheus, metrics` (на практике ещё нужен `health`).
- `management.endpoint.health.show-details: always`.
- `management.metrics.export.prometheus.enabled: true`.
- `management.metrics.distribution.percentiles-histogram` — включён для HTTP-запросов.

## Ограничения / известный техдолг

- **`MonitoringBeanPostProcessor` закомментирован** (`//@Component`) — AOP-подход не работает, все инкременты делаются **вручную** через `metricService.xxx()`. Аннотация `@Monitoring` существует, но не применяется. Либо активировать BPP, либо удалить вместе с аннотацией.
- **Дублирование «метрики + БД»** — при каждом событии два вызова. OK для текущего объёма, но при росте `monitoring` будет расти линейно. Нужна стратегия TTL или архивации.
- **Нет метрик по каналам доставки** — `firebase`, `telegram`, `zvonok` не инкрементят счётчики успех/провал.
- **Нет метрик по ошибкам HTTP** — только скоруп Actuator'а (http_server_requests).
- **`getRegisterCountToday` и пр. используют `LocalDate.now()`** — таймзона сервера, потенциальные сдвиги на границах суток.
- **`show-details: always`** для `/actuator/health` — в prod показывает детали всем (пароли БД не светятся, но статусы компонентов — да).
- **Grafana-дашборды не закоммичены** в репозиторий — нельзя воспроизвести.
- **Нет tracing** (OpenTelemetry, Sleuth) — при падении отследить цепочку нельзя.

## Ссылки

- Связанные фичи: [F1](../FEATURES.md) (`register`), [F2](../FEATURES.md) (`activateQr`), [F5](../FEATURES.md) (`sendNotification`, `readNotification`), [F8](../FEATURES.md) (админ-дашборд в Telegram).
- Код: `backend/src/main/java/ru/car/service/MetricService.java`, `backend/src/main/java/ru/car/monitoring/`.
- Конфиг: `backend/src/main/resources/application.yml` (блок `management:`).
