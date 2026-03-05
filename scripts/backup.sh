#!/bin/bash

# Скрипт для создания бэкапа базы данных PostgreSQL в Docker

# Настройки
CONTAINER_NAME="rectification-db"
DB_NAME="rectification_db"
DB_USER="postgres"
BACKUP_DIR="./backups"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/backup_${DATE}.sql"

# Создаем директорию для бэкапов, если её нет
mkdir -p "$BACKUP_DIR"

echo "Создание бэкапа базы данных: $DB_NAME"

# Выполняем дамп базы данных
docker exec -i "$CONTAINER_NAME" pg_dump -U "$DB_USER" -Fc "$DB_NAME" > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "Бэкап успешно создан: $BACKUP_FILE"
    
    # Удаляем бэкапы старше 30 дней
    find "$BACKUP_DIR" -name "backup_*.sql" -mtime +30 -delete
    echo "Старые бэкапы (старше 30 дней) удалены"
else
    echo "Ошибка при создании бэкапа!"
    exit 1
fi
