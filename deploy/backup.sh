#!/bin/bash
# Ежедневный backup PostgreSQL для Car-ID.
# Запуск через systemd: deploy/systemd/backup.service + backup.timer.
# Креды PG — в /root/.pgpass (chmod 600).
# Соединение — через env vars (см. deploy/README.md).

set -euo pipefail

DB_NAME="${CAR_ID_DB_NAME:?CAR_ID_DB_NAME required}"
DB_USER="${CAR_ID_DB_USER:?CAR_ID_DB_USER required}"
DB_HOST="${CAR_ID_DB_HOST:?CAR_ID_DB_HOST required}"
DB_PORT="${CAR_ID_DB_PORT:?CAR_ID_DB_PORT required}"
BACKUP_DIR="${CAR_ID_BACKUP_DIR:-/var/www/car_id/backups}"
MAX_BACKUPS="${CAR_ID_MAX_BACKUPS:-7}"

CURRENT_DATE=$(date +"%Y-%m-%d:%H-%M-%S")
BACKUP_FILE="$BACKUP_DIR/backup_$CURRENT_DATE.sql"

# Binary compressed dump
pg_dump -U "$DB_USER" -h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME" -F c -f "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "Бэкап успешно создан: $BACKUP_FILE"
else
    echo "Ошибка при создании бэкапа!" >&2
    exit 1
fi

# Ротация: оставляем $MAX_BACKUPS последних валидных (non-zero) бэкапов.
# 0-байтные файлы от неудачных попыток игнорируются и в счёт не идут.
find "$BACKUP_DIR" -maxdepth 1 -name 'backup_*.sql' -size +0 -type f -printf '%T@ %p\n' \
    | sort -rn \
    | tail -n +$((MAX_BACKUPS + 1)) \
    | cut -d' ' -f2- \
    | while read -r OLD_BACKUP; do
        echo "Удаление самого старого бэкапа: $OLD_BACKUP"
        rm -f "$OLD_BACKUP"
    done

echo "Скрипт завершён."
