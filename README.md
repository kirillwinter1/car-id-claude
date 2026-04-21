# Car-ID

Сервис QR-меток для автомобилей. Владелец размещает QR-наклейку на авто — любой человек может отсканировать её и отправить уведомление.

## Структура

```
car-id-claude/
├── backend/      Spring Boot 3.2.3 (Java 17) — API
├── mobile/       Flutter (Dart 3.1+) — iOS/Android
├── frontend/     Vanilla HTML/CSS/JS — веб-сайт
├── ai-ru/        Документация (единый источник правды)
└── CLAUDE.md     Инструкции для разработки
```

## Быстрый старт

```bash
cd backend && ./gradlew bootRun                  # http://localhost:8081
cd mobile && flutter pub get && flutter run
cd frontend && python3 -m http.server 8000
```

## Документация

Вся документация — в [`ai-ru/`](ai-ru/README.md):

- [README](ai-ru/README.md) — обзор проекта
- [FEATURES](ai-ru/FEATURES.md) — список фич со статусами + карточки
- [ARCHITECTURE](ai-ru/ARCHITECTURE.md) — карта кода
- [RULES](ai-ru/RULES.md) — бизнес-правила + правила разработки
- [TECH_DEBT](ai-ru/TECH_DEBT.md) — технический долг
- [OPERATIONS](ai-ru/OPERATIONS.md) — операционный runbook
- [PRODUCT_REVIEW](ai-ru/PRODUCT_REVIEW.md) — продуктовый анализ
- [CLAUDE.md](CLAUDE.md) — инструкции для разработки (в корне)

## Лицензия

Proprietary.
