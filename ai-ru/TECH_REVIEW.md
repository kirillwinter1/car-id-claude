# Технический обзор Car-ID

> Анализ архитектуры: сильные и слабые стороны, риски, план улучшений

---

## Сильные стороны

### Backend

| Аспект | Описание |
|--------|----------|
| **Мультиканальная доставка** | MessageService фасад с 6 каналами (Push, Telegram, WhatsApp, SMS, Call, Email). Асинхронная отправка через ExecutorService (8 потоков). |
| **Шифрование конфигов** | Jasypt для sensitive данных в production. Правильный подход к secrets management. |
| **Liquibase миграции** | Версионирование схемы БД, возможность rollback. 16 changelog файлов. |
| **Prometheus метрики** | Мониторинг из коробки через Actuator. Кастомные метрики через @Monitoring annotation. |
| **Централизованная обработка ошибок** | RestExceptionHandler (99 LOC), понятные HTTP коды, структурированные ответы. |
| **Scheduler для cleanup** | Автоудаление временных QR (каждые 2 часа), черновиков (5 минут). |
| **Чистая пакетная структура** | Логичное разделение по слоям: controller → service → repository → model. |

### Mobile

| Аспект | Описание |
|--------|----------|
| **GetX архитектура** | Консистентный подход: DI, routing, state management в одном фреймворке. |
| **Offline-first токен** | GetStorage сохраняет сессию, восстановление при перезапуске. |
| **Version control** | Force/Soft update диалоги на splash screen. Контроль минимальной версии. |
| **Multi-store** | Поддержка Google Play, Huawei AppGallery, RuStore, App Store. |
| **QR сканирование** | Встроенный сканер с фонариком, валидация формата URL. |

### Frontend

| Аспект | Описание |
|--------|----------|
| **Zero dependencies** | Vanilla JS, нет node_modules, быстрая загрузка, нет уязвимостей npm. |
| **Progressive UX** | Polling статуса, skeleton loading, модальные окна для feedback. |
| **Mobile-first CSS** | Responsive дизайн, современный CSS (Grid, Flexbox, CSS variables). |

---

## Слабые стороны и риски

### Критичные (Security)

| Проблема | Риск | Текущее состояние | Рекомендация |
|----------|------|-------------------|--------------|
| **JWT без expiration** | Украденный токен работает вечно | `JwtService.java:65` — нет `.setExpiration()` | Добавить exp claim (15-30 мин) + refresh token |
| **CORS: `*`** | XSS атаки с любого домена | `SecurityConfiguration.java` — `allowedOrigins("*")` | Whitelist: `car-id.ru`, `localhost` |
| **Admin через конфиг** | Hardcoded credentials | `application.yml` — phone/code для админа | OAuth/RBAC для админки |
| **Нет rate limiting** | DDoS, brute force на login | Отсутствует | Spring Cloud Gateway / bucket4j |
| **HTTP для внешних сервисов** | Man-in-the-middle | Некоторые URL без HTTPS | Только HTTPS |

### Тесты (КРИТИЧНО)

| Компонент | Файлов | Строк | Статус | Покрытие |
|-----------|--------|-------|--------|----------|
| **Backend** | 2 | 145 | **Закомментированы** | ~0% |
| **Mobile** | 1 | 30 | Дефолтный Flutter test | 0% |
| **Frontend** | 0 | 0 | Отсутствуют | 0% |

**Детали:**

- `CarIdApplicationTests.java` — `@SpringBootTest` и `@Test` **закомментированы**
- `IntegrationTest.java` — Selenide E2E тест, `@Test` **закомментирован**, использует внешний URL
- `widget_test.dart` — стандартный counter test из шаблона Flutter, **не относится к проекту**

**Риски отсутствия тестов:**
- Регрессии при изменениях
- Нет уверенности в работоспособности
- Невозможно безопасно рефакторить
- Нет документации поведения через тесты

### Архитектурные

| Проблема | Последствия | Рекомендация |
|----------|-------------|--------------|
| **JdbcTemplate везде** | Много boilerplate (~500 строк SQL), нет type safety, ручной маппинг | Spring Data JPA для CRUD, JdbcTemplate для сложных запросов |
| **Нет API versioning** | Breaking changes сломают клиентов | `/api/v1/`, `/api/v2/` |
| **POST для всех endpoints** | Не RESTful, нет HTTP кэширования, неочевидно какие операции read-only | GET для чтения, POST/PUT/DELETE для записи |
| **Нет OpenAPI спецификации** | Документация устаревает, ручная интеграция | SpringDoc + автогенерация клиентов |
| **Монолит** | Масштабирование только целиком | Ок для текущего размера, но messaging можно вынести в отдельный сервис |
| **Нет транзакций** | Возможны inconsistent состояния | `@Transactional` для операций с несколькими таблицами |

### Mobile

| Проблема | Последствия | Рекомендация |
|----------|-------------|--------------|
| **Нет offline queue** | Действия теряются без сети | Очередь в GetStorage, retry при восстановлении |
| **Один язык (ru)** | Нет масштабирования на другие рынки | i18n инфраструктура есть, добавить en |
| **Firebase обязателен** | Huawei без GMS не получает push | HMS Push Kit для Huawei |
| **Нет error tracking** | Crashlytics есть, но не настроен полностью | Sentry или Firebase Crashlytics с breadcrumbs |

### Frontend

| Проблема | Последствия | Рекомендация |
|----------|-------------|--------------|
| **Polling каждые 5 сек** | Нагрузка на сервер при большом числе пользователей | WebSocket или Server-Sent Events |
| **Нет минификации** | Больший размер JS/CSS | Простой build step (esbuild, terser) |
| **:has() селектор** | Не работает в Safari <15.4, старых браузерах | Fallback или polyfill |
| **Нет CSP headers** | XSS уязвимости | Content-Security-Policy в nginx |

### Инфраструктурные

| Проблема | Последствия | Рекомендация |
|----------|-------------|--------------|
| **Нет CI/CD** | Ручной деплой, человеческие ошибки | GitHub Actions для build/test/deploy |
| **Нет Docker** | Сложный деплой, "works on my machine" | Dockerfile для каждого компонента |
| **Нет health checks для интеграций** | Не видно, что Telegram/SMS упал | Health indicators для каждого канала |
| **Логи в stdout** | Сложно искать и анализировать | Structured logging (JSON), ELK/Loki |
| **Нет backup стратегии** | Потеря данных | pg_dump cron + S3 |

---

## План тестирования (приоритет)

### Фаза 1 — Unit тесты Backend (критично)

| Приоритет | Компонент | Что тестировать |
|-----------|-----------|-----------------|
| **P0** | `JwtService` | Генерация, валидация, expiration |
| **P0** | `NotificationService` | Создание, статусы, фильтрация |
| **P0** | `QrService` | CRUD, статусы, привязка к пользователю |
| **P1** | `MessageService` | Выбор канала, fallback логика |
| **P1** | `AuthenticationCodeService` | Генерация, верификация кодов |
| **P2** | Repositories | CRUD операции, пагинация |

**Инструменты:** JUnit 5, Mockito, AssertJ, H2 (in-memory)

### Фаза 2 — Integration тесты Backend

| Приоритет | Что тестировать |
|-----------|-----------------|
| **P0** | API endpoints (MockMvc) |
| **P0** | Security фильтры |
| **P1** | Liquibase миграции |
| **P1** | Полный flow: регистрация → QR → уведомление |

**Инструменты:** @SpringBootTest, TestContainers (PostgreSQL), MockMvc

### Фаза 3 — Mobile тесты

| Приоритет | Что тестировать |
|-----------|-----------------|
| **P0** | Repositories (API клиенты) — mock responses |
| **P1** | Controllers — state management |
| **P2** | Widget тесты для ключевых экранов |

**Инструменты:** flutter_test, mockito, http_mock_adapter

### Фаза 4 — E2E тесты

| Приоритет | Что тестировать |
|-----------|-----------------|
| **P1** | Web flow: QR → уведомление → статус |
| **P2** | Mobile flow: регистрация → добавление QR |

**Инструменты:** Playwright (web), Appium/Maestro (mobile)

---

## Приоритетный план улучшений

### Фаза 1 — Security (срочно, 1-2 недели)

1. ✗ JWT expiration + refresh token
2. ✗ CORS whitelist
3. ✗ Rate limiting на login endpoints
4. ✗ CSP headers

### Фаза 2 — Тесты (критично, 2-4 недели)

1. ✗ Unit тесты для core сервисов (JwtService, NotificationService, QrService)
2. ✗ Integration тесты для API endpoints
3. ✗ Раскомментировать и исправить существующие тесты
4. ✗ CI pipeline с обязательным прохождением тестов

### Фаза 3 — DevOps (2-3 недели)

1. ✗ Dockerfile для backend
2. ✗ Dockerfile для frontend (nginx)
3. ✗ docker-compose.yml для локальной разработки
4. ✗ GitHub Actions: build → test → deploy

### Фаза 4 — Архитектура (ongoing)

1. ✗ API versioning (`/api/v1/`)
2. ✗ OpenAPI спецификация
3. ✗ WebSocket для статуса уведомлений
4. ✗ Health checks для внешних сервисов

### Фаза 5 — Reliability (ongoing)

1. ✗ Circuit breaker для интеграций (Resilience4j)
2. ✗ Structured logging (JSON)
3. ✗ Alerting (Prometheus + Alertmanager)
4. ✗ Backup стратегия

---

## Метрики качества

### Текущее состояние

| Метрика | Значение | Целевое |
|---------|----------|---------|
| Test coverage (Backend) | ~0% | >70% |
| Test coverage (Mobile) | 0% | >50% |
| Test coverage (Frontend) | 0% | >30% |
| Security issues | 5 критичных | 0 критичных |
| CI/CD | Нет | Полный pipeline |
| Docker | Нет | Все компоненты |

### Итоговая оценка

| Аспект | Оценка | Комментарий |
|--------|--------|-------------|
| **Функциональность** | 8/10 | Всё работает, мультиканальность |
| **Security** | 4/10 | JWT без expiration, CORS *, нет rate limiting |
| **Тесты** | 1/10 | Фактически 0%, все тесты закомментированы |
| **Масштабируемость** | 6/10 | Монолит, но для текущей нагрузки ок |
| **Maintainability** | 6/10 | Понятная структура, но нет тестов для рефакторинга |
| **DevOps** | 3/10 | Нет CI/CD, Docker, автоматизации |

**Общая оценка: 4.7/10**

**Вердикт:** Рабочий MVP с хорошей бизнес-логикой, но критически не хватает тестов и есть серьёзные security issues. Первый приоритет — security fixes и базовое покрытие тестами.

---

## Ссылки

- [ARCHITECTURE.md](ARCHITECTURE.md) — детальная архитектура
- [RULES.md](RULES.md) — бизнес-правила и правила разработки
- [../CLAUDE.md](../CLAUDE.md) — инструкции для разработки
