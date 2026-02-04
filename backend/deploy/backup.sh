#!/bin/bash

# Параметры подключения к PostgreSQL
DB_NAME="your_database_name"
DB_USER="your_username"
DB_HOST="localhost"
DB_PORT="3456"
BACKUP_DIR="/var/www/car_id/backups"  # Путь к папке для бэкапов
MAX_BACKUPS=7  # Максимальное количество бэкапов, которые нужно хранить


# Устанавливаем переменную PGPASSWORD, чтобы передать пароль
#export PGPASSWORD="$DB_PASSWORD"

# Текущая дата для названия бэкапа
CURRENT_DATE=$(date +"%Y-%m-%d:%H-%M-%S")

# Имя файла бэкапа
BACKUP_FILE="$BACKUP_DIR/backup_$CURRENT_DATE.sql"

# Создание бэкапа в текстовый формат весит в 2+ раза больше
#pg_dump -U "$DB_USER" -h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME" -F p -f "$BACKUP_FILE"
# Создание бэкапа в кастомный бинарный сжатый формат
pg_dump -U "$DB_USER" -h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME" -F c -f "$BACKUP_FILE"

#восстановление
#pg_restore -U "$DB_USER" -h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME" "$BACKUP_FILE"


# Проверка успешности бэкапа
if [ $? -eq 0 ]; then
    echo "Бэкап успешно создан: $BACKUP_FILE"
else
    echo "Ошибка при создании бэкапа!"
    exit 1
fi

# Удаление самого старого бэкапа, если количество бэкапов больше $MAX_BACKUPS
BACKUPS_COUNT=$(ls -1 $BACKUP_DIR/backup_*.sql | wc -l)

if [ "$BACKUPS_COUNT" -gt "$MAX_BACKUPS" ]; then
    OLDEST_BACKUP=$(ls -1t $BACKUP_DIR/backup_*.sql | tail -n 1)
    echo "Удаление самого старого бэкапа: $OLDEST_BACKUP"
    rm "$OLDEST_BACKUP"
fi

# Очищаем переменную PGPASSWORD
#qunset PGPASSWORD

echo "Скрипт завершён."