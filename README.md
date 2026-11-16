# warehouse-spring-demo

Демонстрационный backend учёта склада на **Spring Boot**: REST API, JWT-аутентификация, роли и права, справочник единиц измерения и простой веб-интерфейс.

Репозиторий: [github.com/evgenyzhulpa/warehouse-spring-demo](https://github.com/evgenyzhulpa/warehouse-spring-demo)

## Что умеет

- вход по логину и паролю, access + refresh JWT, выход с отзывом refresh-токена
- пользователи, роли и права (`FULL_ACCESS`, чтение/запись пользователей, ролей и единиц измерения)
- CRUD единиц измерения (`units`) с мягким удалением
- Liquibase-миграции схемы и стартовые данные (пользователь `admin`)
- UI на статике (`/`) — вход, разделы, работа с API из браузера

Складской учёт товаров, документов движения и остатков в этой версии ещё не реализован: каркас и админская часть (доступ + единицы измерения).

## Стек

| | |
|---|---|
| Java | 17 |
| Spring Boot | 3.5.5 |
| Сборка | Gradle 9.2.1 (wrapper) |
| БД | PostgreSQL 15 |
| Миграции | Liquibase |
| Безопасность | Spring Security, JWT (jjwt 0.13), BCrypt |
| Прочее | JPA/Hibernate, Validation, MapStruct, Lombok |

## Требования

- JDK 17+
- Docker (для PostgreSQL) или свой экземпляр Postgres
- Gradle Wrapper в репозитории — отдельная установка Gradle не нужна

## Быстрый старт

### 1. PostgreSQL

Параметры приложения заданы в `src/main/resources/application.yaml`:

- хост: `localhost:5432`
- база: `app_db`
- пользователь и пароль — те же, что в yaml (`spring.datasource`)

Поднимите Postgres из каталога `docker/`. Файл `docker/.env` в git не коммитится — создайте его рядом с `docker-compose.yaml`:

```env
DB_USER=admin
DB_PASSWORD=<пароль из application.yaml>
DB_NAME=app_db
```

Значения **должны совпадать** с `spring.datasource` в yaml.

```bash
cd docker
docker compose up -d
```

Контейнер: `warehouse-postgres`, данные в volume `postgres_data`.

### 2. Запуск приложения

Из корня проекта:

```bash
./gradlew bootRun
```

Windows (PowerShell / cmd):

```bat
gradlew.bat bootRun
```

Схема и стартовые данные накатываются Liquibase при старте (`ddl-auto: none`).

- UI: [http://localhost:8080](http://localhost:8080)
- API: `http://localhost:8080/...`

### 3. Демо-пользователь

После миграций доступен системный пользователь:

| Логин | Пароль | Роль |
|---|---|---|
| `admin` | `admin` | Полные права (`FULL_ACCESS`) |

Смените пароль, если поднимаете не только локально.

## Аутентификация

Сессии нет: `STATELESS` + JWT.

1. `POST /login/auth` — логин и пароль, в ответе `accessToken` и `refreshToken`.
2. Дальше заголовок `Authorization: Bearer <accessToken>`.
3. Access живёт **15 минут**, refresh — **7 дней** (`app.jwt` в yaml).
4. `POST /login/refresh` — новый access и новый refresh (ротация).
5. `POST /login/logout` — отзыв refresh-токена.

Открыты без токена: `/login/**`, статика (`/`, `/index.html`, `/css/**`, `/js/**`, `/images/**`, `/favicon.svg`). Остальные запросы требуют аутентификации; методы контроллеров дополнительно проверяют права через `@PreAuthorize`.

Пример входа:

```bash
curl -X POST http://localhost:8080/login/auth ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin\"}"
```

```json
{
  "accessToken": "...",
  "refreshToken": "..."
}
```

## API

Базовый URL: `http://localhost:8080`. Тела — JSON.

### Авторизация (`/login`)

| Метод | Путь | Тело | Описание |
|---|---|---|---|
| POST | `/login/auth` | `{ "username", "password" }` | Выдать токены |
| POST | `/login/refresh` | `{ "refreshToken" }` | Обновить пару токенов |
| POST | `/login/logout` | `{ "refreshToken" }` | Отозвать refresh |

### Пользователи (`/users`)

Права: `USERS_READ` / `USERS_WRITE` / `FULL_ACCESS`. `/users/me` — любому аутентифицированному.

| Метод | Путь | Права | Описание |
|---|---|---|---|
| GET | `/users/` | чтение | Список |
| GET | `/users/me` | — | Текущий пользователь |
| GET | `/users/{id}` | чтение | По id |
| POST | `/users/` | запись | Создать |
| PATCH | `/users/{id}` | запись | Изменить |
| PATCH | `/users/{id}/change-password` | запись | Сменить пароль |
| PATCH | `/users/{id}/mark-deleted` | запись | Мягкое удаление |
| PATCH | `/users/{id}/unmark-deleted` | запись | Снять пометку |
| DELETE | `/users/{id}` | запись | Удалить |

Создание:

```json
{
  "username": "operator",
  "password": "secret",
  "lastName": "Иванов",
  "firstName": "Иван",
  "middleName": "Иванович",
  "rolesIdentifiers": [1]
}
```

### Роли (`/roles`)

Права: `ROLES_READ` / `ROLES_WRITE` / `FULL_ACCESS`.

| Метод | Путь | Описание |
|---|---|---|
| GET | `/roles/` | Список |
| GET | `/roles/{id}` | По id |
| POST | `/roles/` | Создать `{ "name", "permissionsIdentifiers": [1, 2] }` |
| PATCH | `/roles/{id}` | Изменить |
| PATCH | `/roles/{id}/mark-deleted` | Мягкое удаление |
| PATCH | `/roles/{id}/unmark-deleted` | Снять пометку |
| DELETE | `/roles/{id}` | Удалить |

### Права (`/permissions`)

Только чтение. Права: `ROLES_READ` / `ROLES_WRITE` / `FULL_ACCESS`.

| Метод | Путь |
|---|---|
| GET | `/permissions/` |
| GET | `/permissions/{id}` |

Коды: `FULL_ACCESS`, `UNITS_READ`, `UNITS_WRITE`, `USERS_READ`, `USERS_WRITE`, `ROLES_READ`, `ROLES_WRITE`.

### Единицы измерения (`/units`)

Права: `UNITS_READ` / `UNITS_WRITE` / `FULL_ACCESS`.

| Метод | Путь | Описание |
|---|---|---|
| GET | `/units/` | Список |
| GET | `/units/{id}` | По id |
| POST | `/units/` | Создать `{ "name", "code", "description" }` |
| PATCH | `/units/{id}` | Изменить |
| PATCH | `/units/{id}/mark-deleted` | Мягкое удаление |
| PATCH | `/units/{id}/unmark-deleted` | Снять пометку |
| DELETE | `/units/{id}` | Удалить |

## Структура проекта

```
src/main/java/com/warehouse/demo/
  controller/     REST
  service/        бизнес-логика
  repository/     Spring Data JPA
  model/          сущности
  dto/            request / response
  mapper/         MapStruct
  security/       JWT, principal, refresh
  exception/      ошибки API
src/main/resources/
  application.yaml
  db/changelog/   Liquibase
  static/         UI
docker/           PostgreSQL
```

Миграции подключаются из `db/changelog/db.changelog-master.xml`.

## Сборка и тесты

```bash
./gradlew build
./gradlew test
```

Артефакт: `build/libs/` после `bootJar` / `build`.

## Конфигурация

Основные ключи в `application.yaml`:

- `spring.datasource` — JDBC
- `spring.liquibase.change-log` — мастер-changelog
- `server.port` — по умолчанию `8080`
- `app.jwt.secret` — ключ подписи JWT
- `app.jwt.token_expiration` / `app.jwt.refresh_expiration`

Для учебного репозитория секреты лежат в yaml. Для любого окружения кроме локальной машины вынесите datasource и `app.jwt.secret` в переменные окружения или профиль, не коммитьте боевые пароли.

## Лицензия

Учебный pet-проект. Лицензионный файл в репозитории не прилагается.
