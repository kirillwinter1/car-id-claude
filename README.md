# Car-ID

Сервис QR-меток для автомобилей. Владелец размещает QR-наклейку на авто — любой человек может отсканировать её и отправить уведомление.

## Структура монорепозитория

```
car-id-claude/
├── backend/      # Spring Boot 3.2.3 (Java 17) — API сервер
├── mobile/       # Flutter (Dart 3.1+) — iOS/Android приложение
├── frontend/     # HTML/CSS/JS — веб-сайт
├── ai-ru/        # Документация на русском
└── CLAUDE.md     # Инструкции для разработки
```

## Технологии

| Компонент | Стек |
|-----------|------|
| Backend | Java 17, Spring Boot 3.2.3, PostgreSQL, Liquibase |
| Mobile | Flutter, GetX, Firebase |
| Frontend | HTML5, CSS3, Vanilla JavaScript |

## Быстрый старт

### Backend
```bash
cd backend && ./gradlew bootRun
```

### Mobile
```bash
cd mobile && flutter pub get && flutter run
```

### Frontend
```bash
cd frontend && python3 -m http.server 8000
```

## Документация

- [CLAUDE.md](CLAUDE.md) — инструкции для разработки
- [ai-ru/README.md](ai-ru/README.md) — обзор проекта
- [ai-ru/ARCHITECTURE.md](ai-ru/ARCHITECTURE.md) — архитектура
- [ai-ru/RULES.md](ai-ru/RULES.md) — бизнес-правила

## Лицензия

Proprietary
