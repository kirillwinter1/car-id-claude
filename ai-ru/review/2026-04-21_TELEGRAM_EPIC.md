# Phase 2: Telegram-бот как второй клиент — мастер-документ эпика

**Статус:** 📋 Planned · **Заведён:** 2026-04-21 · **Связан с:** [BF1](../backlog/BF1_TELEGRAM_BOT_UI.md), [ROADMAP Phase 2](../ROADMAP.md), [TECH_DEBT A1–A4](../TECH_DEBT.md)

## Контекст

BF1 изначально описывал Phase 2 как «рефакторинг + HTML/эмодзи». По ходу брейнсторма 2026-04-21 скоуп пересмотрен: **Telegram-бот должен стать копией мобильного приложения** — те же экраны, те же команды, те же переходы, — и оставаться каналом доставки уведомлений. Архитектура должна учитывать будущие домены (Phase 5 — аренда машиномест, [BF4](../backlog/BF4_PARKING_RENTAL_EPIC.md)) через расширяемый механизм «сцен», без правок ядра бота.

**Админ-функциональность (`/registerUserCount` и пр.) удаляется** — числа уже доступны в Prometheus/Grafana, поднятых на проде (см. [TECH_DEBT P14](../TECH_DEBT.md)).

## Почему эпик, а не один спек

Полный объём — ~1000 LoC нового кода + 6 переписанных экранов + миграция БД (возможно) + интеграция с мобайлом. В одном спеке это:
- ревьюить невозможно (diff на 2–3 недели работы);
- регрессии негде ловить (всё двигается одновременно);
- блокирует прочие задачи.

Эпик разложен на **пять последовательных под-спеков**. Каждый — отдельный `review/`-документ, отдельный план реализации, отдельный merge.

## Карта экранов: мобайл → бот

| Экран мобайла                   | Соответствующая Scene в боте      | Появляется в |
|---------------------------------|-----------------------------------|--------------|
| Home (главное меню)             | `HomeMenuScene`                   | 2.1 (скелет), 2.2 (полноценный) |
| Уведомления — список            | `NotificationListScene`           | 2.2          |
| Уведомление — детали            | `NotificationDetailsScene` + карточка | 2.5       |
| Метки (QR) — список             | `QrListScene`                     | 2.1 (perf-preserve), 2.2 (красиво) |
| Метка — детали (PDF, отключить) | `QrDetailsScene`                  | 2.2          |
| Временный QR                    | `TemporaryQrScene`                | 2.1 (perf-preserve), 2.2 (редизайн) |
| Настройки уведомлений           | `NotificationSettingsScene`       | 2.2          |
| Сообщить о событии              | `ReportEventScene` (многошаговая) | 2.3          |
| Поддержка / feedback            | `SupportScene`                    | 2.3          |
| Заказать стикер (Ozon/WB)       | `MarketplaceScene`                | 2.3          |
| Профиль                         | `ProfileScene` (номер + выход)    | 2.3          |

Всё, что требует камеры/скана/системных диалогов (`qr_scan`, splash) — из бота исключено осознанно.

## Архитектурная магистраль эпика

```
Telegram Update
   │
   ▼
TelegramBotService (LongPollingBot + Sender + TelegramTransport-impl)
   │ (односторонняя зависимость: non-telegram код видит только Sender)
   ▼
TelegramRouter
   ├─ pre-auth → TelegramAuthorizationService (shareContact или /start-deep-link)
   └─ auth'd → SceneRegistry.get(sceneKey) → scene.handle(callback/text, ctx)
                  │
                  ▼
                  TelegramScene.render(ctx) → SceneOutput {text, keyboard, editInPlace}
                  │
                  ▼
                  TelegramRenderer → TelegramTransport → execute(...)
```

**Ключевые принципы:**

1. **Edit-in-place.** Навигация по меню редактирует одно и то же сообщение (`EditMessageText` / `EditMessageReplyMarkup`). Даёт ощущение «мини-приложения внутри TG». Уведомления приходят отдельными сообщениями — чтобы не перезаписывать историю.
2. **Stateless routing.** Текущая сцена известна из callback_data (`<scene>:<action>[:<args>]`). Бюджет 64 байта. БД-состояние не нужно — переживает рестарт.
3. **Scene-регистрация через Spring DI.** Новая сцена = новый `@Component implements TelegramScene`. `SceneRegistry` собирает `Map<String, TelegramScene>` автоматически. Задел под Phase 5.
4. **Бот — тонкий транспорт.** Никаких `*Repository` напрямую, только через сервисный слой ([TECH_DEBT A3](../TECH_DEBT.md)).
5. **Тексты — в `resources/i18n/telegram_ru.properties`** через Spring `MessageSource`. Задел под i18n.
6. **Telegram остаётся в основном модуле `backend/`**, не выносится в отдельный gradle submodule (решение 2026-04-21: один разраб, нет deployment isolation, другие каналы тоже в основном модуле; граница поддерживается на уровне пакета + публичного `Sender`-интерфейса).

## Декомпозиция на под-спеки

| # | Спек | Видимое пользователю | Документ |
|---|------|----------------------|----------|
| **2.1 ✅** | **Архитектура + Scene-скелет.** Разрыв цикла `Bot↔Logic`, выделение `TelegramTransport`/`TelegramRouter`/`SceneRegistry`/`TelegramMessages`. Миграция текущих команд в сцены **с сохранением текстов и поведения**. Удаление админ-меню. Тесты на роутер + рендер + сцены. | Нет (кроме исчезновения админ-меню) | [spec](2026-04-21_TG_2.1_ARCHITECTURE.md) · [plan](2026-04-21_TG_2.1_PLAN.md) |
| **2.2 ✅** | **Перенос базовых экранов.** Scene'ы Home / Мои метки / Детали метки / Уведомления (список) / Настройки каналов / Профиль. HTML-форматирование, эмодзи, навигация «← Назад» и «🏠 Главное». Edit-in-place. PNG вместо PDF для QR. Auth-welcome через `ReplyKeyboardRemove` + авто-рендер Home. | Да, радикально | [spec](2026-04-21_TG_2.2_BASIC_SCREENS.md) · [plan](2026-04-21_TG_2.2_PLAN.md) |
| **2.3** | **Сообщить о событии + Поддержка + Маркетплейс.** Scene'ы `ReportEventScene` (многошаговая форма), `SupportScene` (текст → feedback), `MarketplaceScene` (ссылки Ozon/WB). | Да | _TBD_ |
| **2.4** | **Deep-link онбординг.** Мобайл: кнопка «Настроить Telegram» → `t.me/<bot>?start=<signed_token>`. Бэк: парсинг payload, связывание без shareContact. | Да (и в мобайле) | _TBD_ |
| **2.5** | **Уведомления-карточки + финал.** Рендер уведомления: HTML-карточка с эмодзи по `reasonDescription`, кнопки «открыть в приложении» / «перейти к метке». Финализация реестра сцен под Phase 5 (ревью, что любой новый домен добавляется без правок ядра). | Да | _TBD_ |

**Зависимости:** 2.1 блокирует всё; 2.2 блокирует 2.5; 2.3, 2.4 — независимы после 2.1 и могут делаться в произвольном порядке.

## Критерий завершения эпика

- Все пять спеков помержены, их критерии выполнены.
- [TECH_DEBT A1–A4](../TECH_DEBT.md) — сняты.
- [F8_TELEGRAM_BOT.md](../features/F8_TELEGRAM_BOT.md) — переписан под новую архитектуру.
- [BF1_TELEGRAM_BOT_UI.md](../backlog/BF1_TELEGRAM_BOT_UI.md) — закрыт.
- Мануальная проверка: пользователь может пройти через бот весь основной сценарий (регистрация → посмотреть метки → скачать PDF → сообщить о событии → настроить каналы → выйти) не заходя в мобайл.

## Что НЕ делаем в этом эпике

- Авторизацию через Telegram Login Widget / OAuth-bot. Это [BF2](../backlog/BF2_MULTI_CHANNEL_AUTH.md), Phase 3.
- Парковочную аренду. Это [BF4](../backlog/BF4_PARKING_RENTAL_EPIC.md), Phase 5. Архитектура лишь оставляет ей дверь.
- Webhook-режим. Long-polling остаётся.
- Мульти-инстанс бота. Один процесс.
- i18n (en). Только задел на уровне `MessageSource`; второй язык — не в этом эпике.

## История обновлений

- 2026-04-21 — эпик заведён, декомпозиция на 2.1–2.5.
- 2026-04-23 — **2.2 закрыт**. `HomeScene` / `QrListScene` v2 / `QrDetailsScene` / `NotificationListScene` / `NotificationSettingsScene` / `ProfileScene` / `TemporaryQrScene` v2 (PNG вместо PDF); удалён легаси `HomeMenuScene`; `TelegramScene.parentKey()` + обработка `:back` в роутере; auth-welcome использует `ReplyKeyboardRemove` и роутер авто-рендерит Home после успешной привязки.
