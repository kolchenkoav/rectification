#!/bin/bash

# Скрипт для создания бэкапа базы данных PostgreSQL в Docker

# Загрузка переменных из .env файла
if [ -f "../.env" ]; then
    export $(cat ../.env | grep -v '^#' | xargs)
fi

# Настройки (из .env или дефолтные)
CONTAINER_NAME=${POSTGRES_HOST:-postgres}
DB_NAME=${POSTGRES_DB:-rectification_db}
DB_USER=${POSTGRES_USER:-postgres}
DB_PASSWORD=${POSTGRES_PASSWORD:-password}
BACKUP_DIR="../scripts/backups"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/backup_${DATE}.sql"

# Создаем директорию для бэкапов, если её нет
mkdir -p "$BACKUP_DIR"

echo "Создание бэкапа базы данных: $DB_NAME"

# Выполняем дамп базы данных
docker exec -i "$CONTAINER_NAME" PGPASSWORD="$DB_PASSWORD" pg_dump -U "$DB_USER" -Fc "$DB_NAME" > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "Бэкап успешно создан: $BACKUP_FILE"
    
    # Удаляем бэкапы старше 30 дней
    find "$BACKUP_DIR" -name "backup_*.sql" -mtime +30 -delete
    echo "Старые бэкапы (старше 30 дней) удалены"
else
    echo "Ошибка при создании бэкапа!"
    exit 1
fi
