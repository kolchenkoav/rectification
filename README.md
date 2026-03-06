# Ректификация спирта 

Веб-приложение для расчёта параметров ректификации спирта.

## Возможности

- Расчёт абсолютного спирта из спирта-сырца
- Расчёт головных фракций и голов
- Расчёт товарного спирта
- Расчёт хвостов
- **История расчётов** с сохранением в PostgreSQL
- **Температурные замеры** в процессе ректификации
- **Печать протокола** ректификации
- Современный адаптивный интерфейс (Bootstrap 5)

## Технологии

- Java 21
- Spring Boot 3.1.5
- Spring Data JPA
- PostgreSQL
- Flyway (миграции)
- Thymeleaf
- Bootstrap 5
- Maven
- Docker / Docker Compose

## Быстрый старт

### С использованием Docker Compose

```bash
# Запуск всех сервисов
docker-compose up -d

# Остановка
docker-compose down
```

Приложение будет доступно по адресу: http://localhost:8089

### Локальный запуск

1. Создайте файл `.env` на основе примера:
```bash
cp .env.example .env
# Отредактируйте .env при необходимости
```

2. Запустите PostgreSQL (или используйте Docker)

3. Сборка и запуск:
```bash
mvn clean package
mvn spring-boot:run
```

## Настройка .env

```env
# PostgreSQL
POSTGRES_DB=rectification_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
POSTGRES_HOST=rectification-db
POSTGRES_PORT=5432

# Spring Boot
SPRING_DATASOURCE_URL=jdbc:postgresql://rectification-db:5432/rectification_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password
SPRING_FLYWAY_REPAIR_ON_MIGRATE=true
```

## Структура проекта

```
src/main/
├── java/com/example/rectificat/
│   ├── controller/      # HTTP-контроллеры
│   ├── model/           # Сущности JPA
│   ├── repository/      # Репозитории
│   ├── services/        # Бизнес-логика
│   └── RectificationApplication.java
└── resources/
    ├── application.yml
    ├── db/migration/    # Flyway миграции
    └── templates/       # HTML-шаблоны Thymeleaf
```

## Скрипты

### Бэкап базы данных

```powershell
cd scripts
.\backup.ps1
```

Бэкапы сохраняются в `scripts/backups/`. Автоматически удаляются бэкапы старше 30 дней.

### Восстановление из бэкапа

```powershell
cd scripts
.\restore.ps1
```

## Тестирование

```bash
# Запуск всех тестов
mvn test
```

## Использование

1. Откройте http://localhost:8089
2. Нажмите "Новый расчёт"
3. Введите параметры:
   - Количество спирта-сырца (л)
   - Крепость спирта (%)
   - Мощность (кВт)
   - Вода в узле отбора (мл)
4. Нажмите "Рассчитать"
5. Просмотрите результаты и добавьте температурные замеры
6. Распечатайте протокол при необходимости
