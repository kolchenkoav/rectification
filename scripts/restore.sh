#!/bin/bash

# Скрипт для восстановления базы данных PostgreSQL из бэкапа

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

echo "=== Восстановление базы данных PostgreSQL ==="
echo ""

# Проверяем, есть ли бэкапы
if [ ! -d "$BACKUP_DIR" ]; then
    echo "Директория с бэкапами не найдена: $BACKUP_DIR"
    exit 1
fi

# Показываем доступные бэкапы
echo "Доступные бэкапы:"
ls -1 "$BACKUP_DIR"/backup_*.sql 2>/dev/null | nl

if [ $? -ne 0 ] || [ -z "$(ls -A $BACKUP_DIR/backup_*.sql 2>/dev/null)" ]; then
    echo "Бэкапы не найдены!"
    exit 1
fi

echo ""
echo -n "Введите номер бэкапа для восстановления (или Enter для последнего): "
read -r CHOICE

if [ -z "$CHOICE" ]; then
    # Используем последний бэкап
    BACKUP_FILE=$(ls -1t "$BACKUP_DIR"/backup_*.sql | head -1)
else
    BACKUP_FILE=$(ls -1 "$BACKUP_DIR"/backup_*.sql | sed -n "${CHOICE}p")
fi

if [ -z "$BACKUP_FILE" ] || [ ! -f "$BACKUP_FILE" ]; then
    echo "Выбранный бэкап не найден!"
    exit 1
fi

echo "Выбран бэкап: $BACKUP_FILE"
echo ""

# Подтверждение
echo -n "ВНИМАНИЕ: Текущие данные будут удалены! Продолжить? (yes/no): "
read -r CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "Отменено пользователем."
    exit 0
fi

echo "Удаление существующей базы данных..."
docker exec -i "$CONTAINER_NAME" PGPASSWORD="$DB_PASSWORD" psql -U "$DB_USER" -c "DROP DATABASE IF EXISTS $DB_NAME;" 2>/dev/null

echo "Создание новой базы данных..."
docker exec -i "$CONTAINER_NAME" PGPASSWORD="$DB_PASSWORD" psql -U "$DB_USER" -c "CREATE DATABASE $DB_NAME;" 2>/dev/null

echo "Восстановление из бэкапа..."
docker exec -i "$CONTAINER_NAME" PGPASSWORD="$DB_PASSWORD" pg_restore -U "$DB_USER" -d "$DB_NAME" -c < "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo ""
    echo "=== База данных успешно восстановлена! ==="
else
    echo "Ошибка при восстановлении базы данных!"
    exit 1
fi

# Показываем доступные бэкапы
echo "Доступные бэкапы:"
ls -1 "$BACKUP_DIR"/backup_*.sql 2>/dev/null | nl

if [ $? -ne 0 ] || [ -z "$(ls -A $BACKUP_DIR/backup_*.sql 2>/dev/null)" ]; then
    echo "Бэкапы не найдены!"
    exit 1
fi

echo ""
echo -n "Введите номер бэкапа для восстановления (или Enter для последнего): "
read -r CHOICE

if [ -z "$CHOICE" ]; then
    # Используем последний бэкап
    BACKUP_FILE=$(ls -1t "$BACKUP_DIR"/backup_*.sql | head -1)
else
    BACKUP_FILE=$(ls -1 "$BACKUP_DIR"/backup_*.sql | sed -n "${CHOICE}p")
fi

if [ -z "$BACKUP_FILE" ] || [ ! -f "$BACKUP_FILE" ]; then
    echo "Выбранный бэкап не найден!"
    exit 1
fi

echo "Выбран бэкап: $BACKUP_FILE"
echo ""

# Подтверждение
echo -n "ВНИМАНИЕ: Текущие данные будут удалены! Продолжить? (yes/no): "
read -r CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "Отменено пользователем."
    exit 0
fi

echo "Удаление существующей базы данных..."
docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -c "DROP DATABASE IF EXISTS $DB_NAME;" 2>/dev/null

echo "Создание новой базы данных..."
docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -c "CREATE DATABASE $DB_NAME;" 2>/dev/null

echo "Восстановление из бэкапа..."
docker exec -i "$CONTAINER_NAME" pg_restore -U "$DB_USER" -d "$DB_NAME" -c < "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo ""
    echo "=== База данных успешно восстановлена! ==="
else
    echo "Ошибка при восстановлении базы данных!"
    exit 1
fi
