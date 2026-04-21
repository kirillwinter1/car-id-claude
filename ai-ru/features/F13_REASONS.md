# F13: Справочник причин уведомления

**Статус:** ✅ В проде · **Последний апдейт:** 2026-04-21

## Что делает

Плоский справочник типовых причин уведомления («Автомобиль мешает», «Произошло ДТП» и т.д.). Используется как радио-кнопки при отправке уведомления с веба ([F4](../FEATURES.md)) и из мобильного приложения. `reason_id` сохраняется в `notification.reason_id`, текст причины рендерится в push/SMS/Telegram.

## Содержимое (текущий seed)

| id | description |
|---:|-------------|
| 1 | Автомобиль мешает |
| 2 | Автомобиль эвакуируют |
| 3 | Произошло ДТП |
| 4 | Открыто окно |
| 5 | Работает сигнализация |
| 6 | Спущено колесо |
| 7 | Мойка завершена |
| 8 | Ремонт завершен |

Засеяно через Liquibase (`changelog-1.1.xml`).

## Сценарии

### Получить список

1. `POST /api/report.get_all_reasons` (mobile, JWT) или `GET /api/report/get_all_reasons` (web, анонимно).
2. `ReasonDictionaryService.findAll` → список `ReasonDictionaryDto { id, description }`.

### Использование при отправке

1. Фронт/mobile присылает `reasonId` в `createDraft` / `report.send` ([F4](../FEATURES.md), [F5](../FEATURES.md)).
2. `NotificationService.create` → `reasonDictionaryService.findByIdOrThrowNotFound(reasonId)` → связывает с `Notification.reason`.
3. Текст причины копируется в `notification.text` **если** пользователь не ввёл кастомный текст (`updateDraft` меняет `text`, только если `dto.text != reason.description`).

## API / Интеграция

| Endpoint | Аутентификация | Описание |
|----------|----------------|----------|
| `POST /api/report.get_all_reasons` | JWT | Для mobile (ответ завернут в `MobileRs`) |
| `GET /api/report/get_all_reasons` | нет | Для веба (простой массив) |

## Реализация

**Backend:**
- `service/ReasonDictionaryService` — только `findAll` и `findByIdOrThrowNotFound`.
- `repository/ReasonDictionaryRepository` — JdbcTemplate.
- `model/ReasonDictionary` — 2 поля: `id`, `description`.
- `mapper/ReasonDictionaryDtoMapper`.

**БД:**
- Таблица `reason_dictionary` (миграция `changelog-1.0.xml`).
- Seed данных — `changelog-1.1.xml`.
- FK `notification.reason_id → reason_dictionary.id`.

**Frontend / Mobile:**
- `frontend/js/sendMsg.js` — строит `<input type="radio">` из списка.
- Mobile `qr_report_event_screen` — выбор причины.

## Ограничения / известный техдолг

- **Одномерный справочник** — нет групп/категорий/иконок/i18n. Все причины показываются сплошным списком.
- **Нет soft-delete или `active` флага** — удалять причину опасно (FK), добавлять можно только миграцией.
- **Нет админского UI/endpoint'а** для управления — только через БД или миграции.
- **Описание хардкодится на русском** — английской локализации нет.
- **Порядок отображения не задан** (только по id) — добавление новой причины в середину списка невозможно без смены id.
- **`findAll` внутри `@Transactional`** — избыточно для read-only запроса.

## Ссылки

- Связанные фичи: [F4](../FEATURES.md) (веб-отправка), [F5](../FEATURES.md) (уведомления).
- Код: `backend/src/main/java/ru/car/service/ReasonDictionaryService.java`, `backend/src/main/resources/db/changelog/changelog-1.1.xml`.
