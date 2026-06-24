# Rectification

![Rectification](Rectification.png "Rectification")

Веб-приложение для расчёта параметров ректификации спирта-сырца: абсолютного спирта, головных фракций, товарного спирта и хвостов. Включает историю расчётов, температурные замеры и печать протокола.

## Возможности

- Расчёт абсолютного спирта, головных фракций, товарного спирта и хвостов
- Сохранение расчётов в PostgreSQL с историей
- Температурные замеры (куб, царга, атмосфера, вода) в процессе ректификации
- Фактические показатели для сравнения с расчётными
- Печать протокола ректификации
- Форма ввода с серверной валидацией
- Адаптивный интерфейс (Bootstrap 5)
- Аутентификация через Spring Security (form login, CSRF)

## Стек технологий

| Компонент | Технология |
|-----------|-----------|
| Язык | Java 17 |
| Фреймворк | Spring Boot 3.2.5 |
| ORM | Spring Data JPA (Jakarta EE) |
| БД | PostgreSQL 15 |
| Миграции | Flyway |
| Шаблонизатор | Thymeleaf |
| Безопасность | Spring Security |
| CSS | Bootstrap 5 |
| Сборка | Maven |
| Контейнеризация | Docker Compose |
| Тестирование | JUnit 5, Mockito, JaCoCo |

## Быстрый старт

### Docker Compose (рекомендуется)

```bash
cp .env.example .env
docker compose up -d
```

Приложение: http://localhost:8089

> Docker Compose привязан к `127.0.0.1` — сервисы не доступны с внешних интерфейсов.

### Локальный запуск

```bash
cp .env.example .env
# Убедитесь, что PostgreSQL запущен и доступен

mvn clean package
mvn spring-boot:run
```

Приложение: http://localhost:8099

## Конфигурация

Все настройки задаются через переменные окружения или файл `.env`:

| Переменная | Описание | По умолчанию |
|-----------|----------|-------------|
| `SPRING_DATASOURCE_URL` | JDBC URL PostgreSQL | `jdbc:postgresql://localhost:5432/rectification_db` |
| `SPRING_DATASOURCE_USERNAME` | Пользователь БД | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Пароль БД | `password` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Режим DDL | `validate` |
| `APP_VERSION` | Версия приложения | `0.0.1` |
| `APP_TAG` | Тег сборки | `SNAPSHOT` |

## Структура проекта

```
src/main/java/com/example/rectification/
├── config/
│   ├── SecurityConfiguration.java      # Spring Security
│   └── GlobalExceptionHandler.java     # Обработка ошибок
├── controller/
│   └── RectificationController.java    # HTTP-эндпоинты
├── model/
│   ├── InData.java                     # DTO входных данных
│   ├── OutData.java                    # DTO результатов расчёта
│   ├── RectificationHistory.java       # Сущность истории расчётов
│   └── Detail.java                     # Сущность температурных замеров
├── repository/
│   ├── RectificationHistoryRepository.java
│   └── DetailRepository.java
├── services/
│   ├── RectificationService.java       # Интерфейс сервиса
│   ├── RectificationServiceImpl.java   # Реализация (CRUD)
│   ├── RectificationCalculator.java    # Расчётная логика
│   └── RectificationConstants.java     # Константы фракций
└── RectificationApplication.java       # Точка входа

src/main/resources/
├── application.yml
├── messages.properties                 # Сообщения валидации
├── db/migration/                       # SQL-миграции Flyway (V1–V6)
├── templates/                          # Шаблоны Thymeleaf
│   ├── InData.html                     # Форма ввода
│   ├── OutData.html                    # Результаты расчёта
│   ├── History.html                    # Список расчётов
│   ├── Print.html                      # Печатный протокол
│   └── error.html                      # Страница ошибки
└── static/
    └── grafik.png
```

## Эндпоинты

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/` | Список расчётов (история) |
| GET | `/new` | Форма нового расчёта |
| POST | `/info` | Выполнить расчёт и сохранить |
| GET | `/view/{id}` | Просмотр расчёта с замерами |
| GET | `/print/{id}` | Печатный протокол |
| POST | `/view/{id}/detail` | Добавить температурный замер |
| POST | `/view/{id}/detail/{detailId}/delete` | Удалить замер |
| POST | `/view/{id}/actual` | Сохранить фактические показатели |
| POST | `/delete/{id}` | Удалить расчёт |
| POST | `/clear` | Очистить всю историю |

## Тестирование

```bash
# Запуск тестов
mvn test

# Полная сборка с тестами и отчётом покрытия
mvn clean test
```

Отчёт JaCoCo: `target/site/jacoco/index.html`

Минимальное покрытие: **80%** строк и ветвлений (настраивается в `pom.xml`).

## Безопасность

- Все страницы и POST-запросы требуют аутентификации
- Статические ресурсы доступны без входа
- CSRF-защита включена
- Для локальной разработки: пользователь `user`, пароль выводится в логах при старте
- Или задайте через env: `SPRING_SECURITY_USER_NAME`, `SPRING_SECURITY_USER_PASSWORD`

> Не коммитьте реальные пароли и секреты.

## Скрипты резервного копирования

```powershell
# Бэкап
cd scripts
.\backup.ps1

# Восстановление
cd scripts
.\restore.ps1
```

Бэкапы: `scripts/backups/` (автоочистка старше 30 дней).

## Лицензия

Лицензия отсутствует. Используйте по своему усмотрению.
