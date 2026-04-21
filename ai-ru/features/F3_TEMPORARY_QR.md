# F3: Временные QR (≈ 2 ч TTL)

**Статус:** ✅ В проде · **Последний апдейт:** 2026-04-21

## Что делает

Пользователь без физической наклейки может одноразово получить временный QR — ссылку `car-id.ru/qr/{uuid}`, которую можно распечатать/показать/оставить под стеклом. QR живёт около 2 часов, потом автоматически удаляется вместе с связанными уведомлениями.

Используется как «срочный режим» (оставил машину на полчаса — распечатал временный QR), и как способ для пользователя без мобильного приложения (например, регистрация через Telegram-бот — см. [F8](../FEATURES.md)).

## Сценарии

### Создание из mobile

1. `POST /api/qr.create_temporary` → `QrService.createTemporaryQr(userId)`.
2. Предусловие: у пользователя **нет** ни одного QR (иначе `ALREADY_HAS_QR`).
3. Создаётся `Qr` в статусе `TEMPORARY`, имя `"Временный qr"`, привязанный к пользователю, `batch_id = 1L`.
4. Возвращается `QrDto` с `qrId`.

### Создание через Telegram-бот

1. Кнопка «Временный QR» в [F8](../FEATURES.md) → `TelegramLogicService.createTemporaryQr`.
2. После создания — сообщение в чате: ссылка `https://car-id.ru/qr/{qrId}` (шаблон `ApplicationConstants.CREATE_TEMPORARY_QR`).

### Ограничения временного QR

- Нельзя привязать к пользователю через `qr.link_to_user` (`ForbiddenException` с кодом `TEMPORARY_QR`).
- Нельзя удалить вручную через `qr.delete` (тот же `ForbiddenException`).
- Веб-отправка уведомления работает — `QrService.findActiveQrById` считает статусы `ACTIVE, TEMPORARY` валидными (см. [F4](../FEATURES.md)).

### Автоудаление

1. `TemporaryQrScheduler` — `@Scheduled(fixedDelay = 2h)`.
2. Внутри — `QrService.destroyAllTemporaryQr`: находит QR со статусом `TEMPORARY`, созданные более **1 часа** назад, удаляет связанные `notification`, затем **жёстко** удаляет записи QR из БД (метод `destroy`, а не soft-delete).
3. Фактический TTL: `[1h, 3h]` — из-за интервала планировщика 2 ч и порога «старше 1 ч».

## API / Интеграция

| Endpoint | Описание |
|----------|----------|
| `POST /api/qr.create_temporary` | Создать временный QR для текущего пользователя |

Telegram-бот: команда `TEMPORARY_QR_CMD` → `createTemporaryQr` ([F8](../FEATURES.md)).

## Реализация

**Backend:**
- `service/QrService.createTemporaryQr` — проверка предусловия + создание.
- `service/QrService.destroyAllTemporaryQr` — уборка.
- `scheduler/TemporaryQrScheduler` — `@Scheduled(fixedDelay = 2, HOURS)`.
- `constants/ApplicationConstants.CREATE_TEMPORARY_QR` — текстовый шаблон для Telegram.

**Mobile:** `lib/screens/qr_marks_screen` (кнопка «создать временный»).

**Frontend:** та же страница `qr.html` — ссылка открывается анонимом и ведёт на форму отправки уведомления ([F4](../FEATURES.md)).

## Ограничения / известный техдолг

- **TTL не соответствует документации.** Код = «старше 1 часа», планировщик = раз в 2 часа. Документация в `CLAUDE.md` и `ARCHITECTURE.md` говорит «2 часа». Нужно привести к одному значению.
- **Предусловие «нет ни одного QR»** — проверяется без фильтра по статусу, т.е. даже `DELETED` блокирует. Должно быть «нет активных или временных».
- **Жёсткое удаление** из БД (`qrRepository.destroy`) нарушает soft-delete-подход остальной системы и исключает аудит.
- **Нет метрики** «сколько временных создано / удалено» (в отличие от `activateQr` для обычных).
- При создании через Telegram — сообщение шлётся всегда без учёта `isAdmin`-меню (формально корректно, но не влияет на ошибки).

## Ссылки

- Связанные фичи: [F2](../FEATURES.md) (обычные QR), [F4](../FEATURES.md) (веб-отправка — работает на временных), [F8](../FEATURES.md) (Telegram).
- Код: `backend/src/main/java/ru/car/service/QrService.java#createTemporaryQr`, `#destroyAllTemporaryQr`; `backend/src/main/java/ru/car/scheduler/TemporaryQrScheduler.java`.
