# Dockerization Car-ID backend

**Дата:** 2026-04-21
**Статус:** 📋 Запланировано (не начато)
**Инициатор:** мотивация пользователя — унификация с соседними приложениями на том же сервере (Lead Board, CRM — уже в Docker), независимость от состояния VM, облегчение CI/CD.

## Контекст

Car-ID backend сейчас запущен как **systemd-сервис** (`car_id.service` → `/bin/bash /var/www/car_id/run.sh` → JVM + jar). На той же prod-машине `79.174.94.70` уже работает Docker с несколькими контейнерами:
- `onelane-backend`, `onelane-frontend` — Lead Board.
- `crm-backend`, `crm-frontend` — внутренний CRM.
- `leadboard-grafana`, `leadboard-prometheus`, `leadboard-alertmanager` — observability stack.

PostgreSQL для Car-ID живёт **на отдельной VM** `79.174.88.176:15965`, значит БД докеризировать не нужно — только само Spring Boot приложение.

## Что даёт переход на Docker

- **Воспроизводимость.** Java 21.0.3 сейчас захардкожена в `run.sh`; при апгрейде JRE на хосте сломается. В образе версия зашита.
- **Изоляция.** Падение приложения не затронет соседей.
- **Port hardening.** В docker-compose можно сразу `127.0.0.1:8081:8081` — закрывает [TECH_DEBT P10](../TECH_DEBT.md).
- **Единый деплой-пайплайн** с Lead Board (там уже есть [`DEPLOY.md`](https://github.com/kirillwinter1/lead-board-claude/blob/main/DEPLOY.md) как референс).
- **Rollback через `docker tag`** проще, чем swap jar + `systemctl restart`.
- **Готово к CI/CD** — можно автоматизировать в GitHub Actions (сейчас backend CI настроен, frontend — нет; dockerization поможет унифицировать).

## Что мешает делать прямо сейчас

- **Диск на prod 81%** (16/20 GB). Каждый Docker image ~300-500 MB + слои. Нужно либо почистить соседние старые image-ы и логи, либо апгрейдить VM — см. [TECH_DEBT P14](../TECH_DEBT.md).
- **RAM 86 MB free, swap активен 492 MB.** Docker overhead ~50 MB на контейнер — не страшно, но ощутимо на перегруженной VM.
- **Car-ID jar на проде от ноября 2025**, миграций Liquibase накопилось 8+ штук. Разумно **совмещать** Docker-переход с ближайшим большим деплоем, а не делать отдельный цикл «просто переехать». Два отдельных риска сливаются в один.
- **Auth-редизайн впереди** — при большом рефакторинге конфигов, Jasypt-схемы, env-vars будет удобнее уже работать в Docker, но делать Dockerization **до** редизайна = дублировать работу по переосмыслению секретов.
- **Backup не тащим в Docker** — `backup.sh` оставляем на хосте (он уже починен, есть и локальная копия через launchd).

## План перехода (когда будем делать)

### Этап 0 — подготовка ресурсов

1. Освободить место на prod-VM или апгрейдить: чистка старых Docker image, ротация Docker log'ов, пересмотреть размер дисков Lead Board + CRM.
2. Убедиться что RAM хватает под +контейнер Car-ID (в идеале — увеличить RAM до 4 GB).

### Этап 1 — артефакты деплоя в репо

Положить в `deploy/` (но **не применять** на проде):

1. `deploy/Dockerfile` — multi-stage: `FROM eclipse-temurin:21-jre` + COPY jar. Entrypoint через env vars.
2. `deploy/docker-compose.prod.yml` — один сервис `car_id`, порт `127.0.0.1:8081:8081`, volumes:
   - `/etc/car_id/application-prod.yml:/config/application-prod.yml:ro`
   - `/var/log/car_id:/var/log/car_id`
   - `/etc/car_id/firebase:/firebase:ro` (сейчас захардкожено `/firebase/car-id-55917-*.json`)
3. `deploy/.env.example` — шаблон env-файла.
4. Обновить `deploy/README.md` — «systemd vs Docker: порядок миграции».
5. `deploy/rollback.sh` — ручной откат на systemd-версию (на всякий случай).

### Этап 2 — переключение prod

Делается вместе с первым же большим деплоем накопившихся изменений (после auth-редизайна):

1. `systemctl stop car_id.service && systemctl disable car_id.service`.
2. `docker compose -f /opt/car_id/docker-compose.prod.yml up -d`.
3. Nginx трогать не нужно — прокси на `127.0.0.1:8081` работает.
4. Smoke: `curl localhost:8081/actuator/health`.
5. Если плохо — `docker compose down && systemctl start car_id.service` (rollback).
6. Если хорошо — `systemctl mask car_id.service` (чтобы случайно не запустился).

### Этап 3 — клин-ап

1. Убрать systemd-unit из репо `deploy/systemd/car_id.service` или пометить как legacy.
2. Обновить `OPERATIONS.md` на Docker-flow.
3. Оставить `backup.service` + `health.service` на systemd — они работают отдельно и правильно.

## Риски и митигация

| Риск | Митигация |
|------|-----------|
| Не стартует контейнер на проде | Параллельно держим systemd-unit отключённым, но готовым (`systemctl start` = быстрый откат) |
| Jasypt не находит пароль | Env var `JASYPT_ENCRYPTOR_PASSWORD` через `docker compose` `environment:` |
| Firebase credentials не подхватываются | Явный `-v /etc/car_id/firebase:/firebase:ro` |
| Логи не пишутся | Volume на `/var/log/car_id`, убедиться что `LOG_PATH` env var указывает туда |
| Docker daemon падает = простой Car-ID | Uptime Docker на машине = 6+ недель, риск низкий; всё равно оставляем systemd-версию как документированный fallback |

## Оценка затрат

- **Подготовка артефактов (этап 1)** — 2-4 часа.
- **Тестирование в dev** — 1-2 часа.
- **Применение на prod (этап 2)** — 30 минут окна простоя.
- **Клин-ап (этап 3)** — 1 час.

**Итого:** рабочий день, большую часть которого делает один человек. Применение = короткое окно, лучше ночью.

## Решение о старте

Не запускаем пока не сложится **связка** с более крупной задачей:
- либо auth-редизайн,
- либо плановый деплой накопленных миграций,
- либо форс-мажор на хосте (OOM, проблема с systemd).

До тех пор этот документ — живая заметка, обновляется при изменении контекста.

## Ссылки

- [TECH_DEBT.md](../TECH_DEBT.md) — пункты P10, P14, P11 напрямую связаны.
- [OPERATIONS.md](../OPERATIONS.md) — текущий runbook (systemd).
- [`../deploy/`](../../deploy/) — где будут лежать Docker-артефакты.
- Референс: `/Users/kirillreshetov/IdeaProjects/lead-board-claude/DEPLOY.md` (Docker-схема для Lead Board).
