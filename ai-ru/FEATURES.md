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

Пока пусто. По мере появления идей — заводятся в [backlog/](backlog/).

## Рефакторинги / ревью

Планируется:
- `review/2026-04-XX_TELEGRAM_REFACTORING.md` — рефакторинг Telegram-бота (см. техдолг A1-A4 в [TECH_DEBT.md](TECH_DEBT.md)).

Активные документы см. [review/](review/).

## Техдолг

Единый реестр → [TECH_DEBT.md](TECH_DEBT.md). Фичевые ограничения — в разделе «Ограничения» каждой карточки.
