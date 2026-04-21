# F14: Marketplaces (ссылки на магазины)

**Статус:** ✅ В проде · **Последний апдейт:** 2026-04-21

## Что делает

Статически возвращает ссылки на товар (QR-наклейки Car-ID) на внешних маркетплейсах — Wildberries и Ozon. Мобильное приложение показывает кнопки «Купить на WB» / «Купить на Ozon», ведя пользователя в магазин.

Хранится в единственной записи таблицы `marketplaces` — «singleton row». Фактически это конфиг-настройка, поднятая до уровня БД.

## Сценарии

### Получить ссылки

1. `POST /api/marketplaces.get` (анонимный, без JWT) → `MarketplaceService.get()` → `findFirst()` → `MarketplacesDto { wb, ozon, activity }`.

## API / Интеграция

| Endpoint | Аутентификация | Описание |
|----------|----------------|----------|
| `POST /api/marketplaces.get` | нет | Получить ссылки WB/Ozon |

## Реализация

**Backend:**
- `controller/MarketplacesController` — один endpoint.
- `service/MarketplaceService` — один метод `get` = `findFirst` + mapper.
- `repository/MarketplacesRepository.findFirst` — берёт **первую** запись из таблицы.
- `model/Marketplaces` — `id`, `wb`, `ozon`, `activity`.

**Mobile:** страница/кнопки «Купить», дёргает endpoint при открытии.

## Ограничения / известный техдолг

- **Единственная запись, но не enforced** — в схеме нет unique/check, что в таблице ровно одна строка. `findFirst` просто берёт первую по SQL-порядку.
- **`activity` флаг не проверяется** ни в сервисе, ни в контроллере — можно «выключить» показ ссылок, но API всё равно их отдаст.
- **Только два маркетплейса** — расширение на Яндекс Маркет/MegaMarket требует схему.
- **Нет админского endpoint'а** для обновления ссылок — только SQL/миграция.
- **Нет кэша** — каждый запрос = SQL-запрос. Таблица из одной строки, но формально кэш уместен.
- **Похоже, это и конфиг (таблица на одну строку), и бизнес-данные** — архитектурный компромисс; в идеале — конфиг в `application.yml` или админская страница.

## Ссылки

- Код: `backend/src/main/java/ru/car/controller/MarketplacesController.java`, `backend/src/main/java/ru/car/service/MarketplaceService.java`.
