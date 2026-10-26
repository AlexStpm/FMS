# Finance Management System

Backend-сервис для учёта финансовых операций, счетов, подразделений, бюджетов и отчётов.

## Технологии

- Java 21, Spring Boot 4.1.1, Maven;
- Spring Web MVC, Spring Data JPA, Hibernate;
- PostgreSQL, Flyway;
- Spring Security, JWT access/refresh tokens, Google OAuth2/OIDC;
- JUnit 5, Mockito, Testcontainers;
- Docker и Docker Compose;
- отдельный модуль `fms-startup-logger-starter`.

## Структура проекта

Проект состоит из Maven-модулей:

```text
fms-parent
├── fms-startup-logger-starter
└── fms-app
    └── src/main/java/org/alexstpm/fms
        ├── account
        ├── audit
        ├── auth
        ├── budget
        ├── category
        ├── counterparty
        ├── department
        ├── report
        ├── security
        ├── transaction
        └── user
```

`fms-app` — основное Spring Boot-приложение. `fms-startup-logger-starter` — собственный стартер, который выводит информацию о запуске приложения.

## Профили

В проекте есть профили:

- `dev` — локальный запуск и разработка;
- `test` — конфигурация автоматических тестов;
- `prod` — запуск Docker-контейнера.

При запуске через Docker Compose активируется профиль `prod`. В тестах используется Testcontainers и отдельный контейнер PostgreSQL.

## Сборка и тесты

Для сборки и запуска тестов нужны Java 21 и работающий Docker Engine. Docker требуется тестам, которые поднимают PostgreSQL через Testcontainers.

Из корня проекта:

```powershell
.\mvnw.cmd clean verify
```

Тесты с Mockito проверяют сервисную логику с помощью mock-объектов. Интеграционный тест запускает Spring-контекст и PostgreSQL в Testcontainers.

## Локальный запуск

Для локального запуска нужны Java 21 и PostgreSQL, доступный по настройкам профиля `dev`.

Из корня проекта:

```powershell
.\mvnw.cmd -pl fms-app -am spring-boot:run -Dspring-boot.run.profiles=dev
```

Параметры локального подключения к базе задаются конфигурацией профиля `dev` или переменными окружения, которые использует этот профиль.

## Запуск через Docker Compose

Для запуска нужны Docker Engine и Docker Compose. Из корня проекта:

```powershell
docker compose config --quiet
docker compose up --build -d
```

Compose запускает два сервиса:

- `db` — PostgreSQL;
- `app` — Spring Boot-приложение.

Приложение доступно на порту `8080`:

```text
http://localhost:8080
```

Внутри Docker-сети приложение подключается к базе по адресу `jdbc:postgresql://db:5432/fms`. Имя `db` — имя сервиса PostgreSQL в Compose; внутри контейнера `localhost` указывал бы на сам контейнер приложения.

Посмотреть состояние сервисов и логи приложения:

```powershell
docker compose ps
docker compose logs -f app
```

Остановить сервисы:

```powershell
docker compose down
```

Данные PostgreSQL сохраняются в Docker-томе `fms_postgres_data`. Команда `docker compose down -v` удаляет этот том вместе с данными.

### Переменные окружения Compose

В корневом `.env` задаются значения, которые Compose подставляет в конфигурацию:

```dotenv
DB_PASSWORD=локальный_пароль_для_postgres
JWT_SECRET=случайная_строка_минимум_32_байта
GOOGLE_CLIENT_ID=google_oauth_client_id
GOOGLE_CLIENT_SECRET=google_oauth_client_secret
```

`JWT_SECRET` должен содержать не менее 32 байт. Не коммитьте `.env` и не используйте примерные локальные значения в production.

Для Docker Compose redirect URI Google OAuth-клиента должен включать:

```text
http://localhost:8080/login/oauth2/code/google
```

## Аутентификация

### Логин и пароль

Вход выполняется запросом:

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "password": "password"
}
```

Ответ содержит `accessToken`, `refreshToken`, `tokenType` и `expiresIn`. Для обновления пары токенов используется:

```http
POST /api/v1/auth/refresh
Content-Type: application/json
```

```json
{
  "refreshToken": "<refresh-token>"
}
```

Для защищённых API передавайте access-токен в заголовке:

```http
Authorization: Bearer <access-token>
```

### Google OAuth2/OIDC

Начало входа:

```text
GET /oauth2/authorization/google
```

Callback:

```text
/login/oauth2/code/google
```

После успешной проверки Google-аккаунта приложение сопоставляет подтверждённый email с существующим активным пользователем и возвращает собственные JWT-токены. Пользователь должен быть заранее создан в системе. Автоматическое создание пользователя при Google-входе не выполняется.

## Роли

В приложении определены роли:

- `ADMIN`;
- `FINANCE_MANAGER`;
- `ACCOUNTANT`;
- `AUDITOR`.

Проверки доступа выполняются в сервисном слое приложения.

## REST API

Прикладные маршруты имеют префикс `/api/v1`.

```text
/api/v1/auth
/api/v1/users
/api/v1/departments
/api/v1/accounts
/api/v1/categories
/api/v1/counterparties
/api/v1/transactions
/api/v1/budgets
/api/v1/reports
/api/v1/audits
```

Примеры маршрутов отчётов:

```http
GET /api/v1/reports/cash-flow?departmentId=1&startDate=2026-01-01&endDate=2026-01-31
GET /api/v1/reports/budget-execution?departmentId=1&startDate=2026-01-01&endDate=2026-01-31
```

Для дат используется формат `yyyy-MM-dd`.

Ошибки API возвращаются в JSON, например:

```json
{
  "error": "ACCESS_DENIED",
  "message": "Access denied"
}
```

## Миграции базы данных

Схема базы создаётся и обновляется миграциями Flyway из:

```text
fms-app/src/main/resources/db/migration
```

Hibernate настроен на `ddl-auto=validate`: приложение проверяет соответствие схемы сущностям, но не создаёт её автоматически.

## Startup logger starter

Модуль `fms-startup-logger-starter` подключён к `fms-app` как Maven-зависимость. При старте приложения он выводит диагностическую информацию, включая профили, версию, параметры источника данных без пароля и длительность запуска.

Логгер можно отключить настройкой:

```properties
app.startup-logger.enabled=false
```
