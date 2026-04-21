# F15: Version Control мобильного клиента

**Статус:** ✅ В проде · **Последний апдейт:** 2026-04-21

## Что делает

Отдаёт мобильному клиенту **минимальную** и **текущую** (актуальную) версии приложения для трёх сторов — App Store, Google Play, RuStore. Mobile использует эти данные, чтобы показать:

- «Доступна новая версия» → мягкое уведомление об обновлении (если `installed < current`).
- «Обновление обязательно» → блокирующий экран (если `installed < min`).

Singleton-row таблица `version_control`, как [F14](../FEATURES.md).

## Сценарии

### Получить версии

1. Mobile при старте: `POST /api/version_control.get` (анонимный, без JWT).
2. Ответ: `{ appleCurrent, appleMin, googleCurrent, googleMin, rustoreCurrent, rustoreMin }`.
3. Mobile сравнивает с локальной версией → решает, показывать ли баннер/экран обновления.

## API / Интеграция

| Endpoint | Аутентификация | Описание |
|----------|----------------|----------|
| `POST /api/version_control.get` | нет | Получить версии для всех сторов |

## Реализация

**Backend:**
- `controller/VersionControlController` — один endpoint.
- `service/VersionControlService` — `get` = `findFirst` + mapper.
- `repository/VersionControlRepository.findFirst` — первая запись.
- `model/VersionControl` — 6 полей (2 на каждый стор).

## Ограничения / известный техдолг

- **Версии — строки, не версионные объекты** — сравнение лежит на клиенте (Mobile), backend не валидирует формат.
- **Нет админского endpoint'a / UI** — обновление только через БД или миграцию при релизе.
- **Нет истории** — при обновлении `current`/`min` предыдущие значения теряются.
- **Все строки публичные** — RuStore может быть нежелательно светить (хотя и безобидно).
- **Не связан с серверным Build/Release** — сервер не знает, какая версия у клиента, пока клиент не запросит; аналитики по версиям нет.
- **Отсутствует поле `releaseNotes`/`message`** — нельзя показать пользователю, что изменилось.
- Похоже на паттерн singleton-row (`findFirst`) — имеет смысл превратить в конфиг.

## Ссылки

- Код: `backend/src/main/java/ru/car/controller/VersionControlController.java`, `backend/src/main/java/ru/car/service/VersionControlService.java`.
