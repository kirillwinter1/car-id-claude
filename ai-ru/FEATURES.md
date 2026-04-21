# Список фич Car-ID

Индекс всех функциональных блоков сервиса. Карточки лежат в [features/](features/), будущее — в [backlog/](backlog/), рефакторинги — в [review/](review/).

## Реализованные (F)

| #   | Название                                 | Слой                        | Карточка |
|-----|------------------------------------------|-----------------------------|----------|
| F1  | Auth по телефону (flashcall + SMS + JWT) | Backend + Mobile            | [F1](features/F1_AUTH.md) |
| F2  | QR-коды: создание, привязка, типы        | Backend + Mobile            | [F2](features/F2_QR_CODES.md) |
| F3  | Временные QR (≈ 2 ч TTL)                 | Backend + Mobile + Telegram | [F3](features/F3_TEMPORARY_QR.md) |
| F4  | Веб-отправка уведомления при сканировании| Backend + Frontend          | [F4](features/F4_WEB_REPORT.md) |
| F5  | Входящие / исходящие уведомления         | Backend + Mobile            | [F5](features/F5_NOTIFICATIONS.md) |
| F6  | Настройки каналов уведомлений            | Backend + Mobile            | [F6](features/F6_NOTIFICATION_SETTINGS.md) |
| F7  | Push-уведомления (Firebase FCM)          | Backend → Mobile            | [F7](features/F7_FIREBASE_PUSH.md) |
| F8  | Telegram-бот (уведомления + команды)     | Backend ↔ Telegram          | [F8](features/F8_TELEGRAM_BOT.md) |
| F10 | SMS (SMS Aero) — резерв для auth         | Backend → User              | [F10](features/F10_SMS.md) |
| F11 | Flashcall / голосовой звонок (Zvonok)    | Backend ↔ User              | [F11](features/F11_ZVONOK.md) |
| F12 | Feedback (поддержка → Telegram)          | Mobile + Backend            | [F12](features/F12_FEEDBACK.md) |
| F13 | Справочник причин уведомления            | Backend + Web               | [F13](features/F13_REASONS.md) |
| F14 | Marketplaces (ссылки на магазины)        | Backend + Mobile            | [F14](features/F14_MARKETPLACES.md) |
| F15 | Version Control мобильного клиента       | Backend + Mobile            | [F15](features/F15_VERSION_CONTROL.md) |
| F16 | Metrics & Monitoring (Actuator + Prometheus) | Backend                 | [F16](features/F16_METRICS_MONITORING.md) |

**Легенда статусов** (используется в карточках):
- ✅ В проде — фича работает.
- ⚠️ Частично используется — код есть, но не во всех сценариях (напр. SMS).
- 📝 Описание TBD — фича в проде, но ретроспективного описания ещё нет.
- 🟡 In progress — пишется.

## Бэклог (BF)

Верхнеуровневый план развития — [ROADMAP.md](ROADMAP.md).

| # | Название | Фаза | Карточка |
|---|----------|------|----------|
| BF1 | Telegram-бот с красивым UI | Phase 2 | [BF1](backlog/BF1_TELEGRAM_BOT_UI.md) |
| BF2 | Мульти-канальная авторизация и уведомления (Telegram / MAX / ...) | Phase 3 | [BF2](backlog/BF2_MULTI_CHANNEL_AUTH.md) |
| BF3 | Редизайн лендинга car-id.ru | Phase 4 | [BF3](backlog/BF3_LANDING_REDESIGN.md) |
| BF4 | Аренда машиномест в ЖК (эпик) | Phase 5 | [BF4](backlog/BF4_PARKING_RENTAL_EPIC.md) |

## Рефакторинги / ревью

Активные документы — в [review/](review/).

Запланированное:
- [2026-04-21_DOCKERIZATION.md](review/2026-04-21_DOCKERIZATION.md) — перевод Car-ID backend на Docker (связка с P10/P11/P14).
- `review/YYYY-MM-DD_TELEGRAM_REFACTORING.md` *(будет)* — рефакторинг Telegram-бота (A1–A4 в [TECH_DEBT.md](TECH_DEBT.md)).
- `review/YYYY-MM-DD_AUTH_REDESIGN.md` *(будет)* — мульти-канальная авторизация (Telegram / MAX / flashcall); в это же окно закрываем P1 (секреты + ротация JWT signing key).

## Техдолг

Единый реестр → [TECH_DEBT.md](TECH_DEBT.md). Фичевые ограничения — в разделе «Ограничения» каждой карточки.
