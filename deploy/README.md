# deploy/ — операционные скрипты и systemd unit-ы Car-ID

> Источник правды для того, что должно лежать на prod-машине рядом с приложением.
> Публичные файлы: секреты (пароли БД, Telegram токены, IP-адреса) **не коммитятся** — передаются через env files в `/etc/car_id/`.

## Структура

```
deploy/
├── README.md              ← этот файл
├── run.sh                 запуск Spring Boot приложения
├── backup.sh              ежедневный pg_dump + ротация
├── health.sh              проверка /actuator/health, алерт при неудаче
├── send_message.sh        отправка сообщения в Telegram (админ-канал)
└── systemd/
    ├── car_id.service     запуск приложения
    ├── backup.service     обёртка над backup.sh
    ├── backup.timer       ежедневно в 03:00
    ├── health.service     обёртка над health.sh
    └── health.timer       каждые 5 секунд
```

## Env-переменные

Скрипты параметризованы; конкретные значения задаются на prod через `EnvironmentFile=-/etc/car_id/*.env`. Значения этих файлов — в приватном `ops.inf`, читаются напрямую на сервере.

### `/etc/car_id/backup.env`

| Переменная | Описание | Обязательность |
|------------|----------|----------------|
| `CAR_ID_DB_NAME` | имя БД | required |
| `CAR_ID_DB_USER` | PG user | required |
| `CAR_ID_DB_HOST` | PG host | required |
| `CAR_ID_DB_PORT` | PG port | required |
| `CAR_ID_BACKUP_DIR` | куда складывать дампы | default: `/var/www/car_id/backups` |
| `CAR_ID_MAX_BACKUPS` | сколько хранить | default: `7` |

Пароль PG — **не через env**, а через `/root/.pgpass` (chmod 600), формат:
```
<host>:<port>:<db>:<user>:<password>
```

### `/etc/car_id/health.env`

| Переменная | Описание | Обязательность |
|------------|----------|----------------|
| `CAR_ID_HEALTH_URL` | URL healthcheck | default: `http://localhost:8081/actuator/health` |
| `CAR_ID_SEND_MESSAGE_SCRIPT` | путь до send_message.sh | default: `/var/www/car_id/send_message.sh` |
| `TELEGRAM_ALERT_BOT_TOKEN` | токен бота (используется `send_message.sh` при вызове) | required |
| `TELEGRAM_ALERT_CHAT_ID` | chat id для алертов | required |

### `/etc/car_id/car_id.env` (для `run.sh`)

| Переменная | Описание | Обязательность |
|------------|----------|----------------|
| `CAR_ID_JAVA_BIN` | путь до `java` | default: `/usr/lib/jvm/jdk-21.0.3/bin/java` |
| `CAR_ID_CONFIG_PATH` | путь до `application-prod.yml` | default: `/var/www/car_id/application-prod.yml` |
| `CAR_ID_JAR_PATH` | путь до jar | default: `/var/www/car_id/jar/car_id.jar` |
| `CAR_ID_PID_FILE` | PID-файл | default: `/var/www/car_id/car_id.pid` |
| `CAR_ID_SEND_MESSAGE_SCRIPT` | путь до send_message.sh | default: `/var/www/car_id/send_message.sh` |

## Процедура разворачивания на новом сервере

1. Создать каталоги:
   ```bash
   mkdir -p /var/www/car_id/{jar,logs,backups}
   mkdir -p /etc/car_id
   ```
2. Положить jar приложения в `/var/www/car_id/jar/car_id.jar` (собран локально через `./gradlew bootJar`).
3. Положить конфиг `/var/www/car_id/application-prod.yml` (с `ENC(...)` — зашифрованный Jasypt; пароль Jasypt передать через env `JASYPT_ENCRYPTOR_PASSWORD` в `car_id.env`).
4. Скопировать скрипты из `deploy/` в `/var/www/car_id/`:
   ```bash
   scp deploy/run.sh deploy/backup.sh deploy/health.sh deploy/send_message.sh root@<host>:/var/www/car_id/
   ssh root@<host> "chmod +x /var/www/car_id/*.sh"
   ```
5. Скопировать systemd unit-ы:
   ```bash
   scp deploy/systemd/*.service root@<host>:/etc/systemd/system/
   scp deploy/systemd/*.timer   root@<host>:/lib/systemd/system/
   ssh root@<host> "systemctl daemon-reload"
   ```
6. Создать env-файлы в `/etc/car_id/` (значения — из `ops.inf`; пример:
   ```
   # /etc/car_id/backup.env
   CAR_ID_DB_NAME=carid
   CAR_ID_DB_USER=dima
   CAR_ID_DB_HOST=...
   CAR_ID_DB_PORT=...
   ```
   ). `chmod 600`.
7. Создать `/root/.pgpass` (chmod 600).
8. Включить сервисы:
   ```bash
   systemctl enable --now car_id.service backup.timer health.timer
   ```
9. Проверить:
   ```bash
   curl -s http://localhost:8081/actuator/health
   systemctl status car_id.service backup.timer health.timer
   ```

## Текущее состояние prod (2026-04-21)

- Prod-машина `79.174.94.70` (hostname `cv3724945.novalocal`).
- Скрипты на проде **не совпадают** с этой версией: на проде используются хардкоды вместо env vars. Эта версия — **желаемое состояние** для следующего деплоя.
- Порядок перехода: задеплоить env-driven скрипты + заполнить `/etc/car_id/*.env` + восстановить `/root/.pgpass` → `systemctl daemon-reload` + `systemctl restart car_id.service backup.timer health.timer`.

## Связанные файлы

- [`../ai-ru/OPERATIONS.md`](../ai-ru/OPERATIONS.md) — runbook (процедуры).
- `ops.inf` (локально у пользователя, вне git) — конкретные значения env vars.
- [`../ai-ru/TECH_DEBT.md`](../ai-ru/TECH_DEBT.md) — задолженности по проду (backup, секреты, DEBUG-level, Swagger public и т.п.).
