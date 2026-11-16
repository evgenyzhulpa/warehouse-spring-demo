# warehouse-spring-demo

Демонстрационный учёт склада на **Spring Boot**: REST API, JWT, роли и права, НСИ и черновики прихода/расхода, простой веб-интерфейс.

Репозиторий: [github.com/evgenyzhulpa/warehouse-spring-demo](https://github.com/evgenyzhulpa/warehouse-spring-demo)

## Что умеет

- вход по логину и паролю, access + refresh JWT, выход с отзывом refresh-токена
- пользователи, роли и права (`FULL_ACCESS` и пары `*_READ` / `*_WRITE`)
- НСИ: единицы измерения, склады, номенклатура (артикул уникален, в том числе у помеченных на удаление)
- черновики поступления и реализации: шапка и строки отдельными запросами, номер документа назначается после записи
- мягкое удаление шапок и справочников
- Liquibase-миграции схемы и стартовые данные (пользователь `admin`, основной склад, товар `ART-001`, документы `000001`)
- UI на статике (`/`) — вход, разделы НСИ и склада, работа с API из браузера

Проведения, статусов документа, регистров остатков и движений, цен и заказов в этой версии нет. Списание и отчёты в интерфейсе — заглушки.

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

Коды: `FULL_ACCESS`, `UNITS_READ`, `UNITS_WRITE`, `WAREHOUSES_READ`, `WAREHOUSES_WRITE`, `PRODUCTS_READ`, `PRODUCTS_WRITE`, `INCOME_DOCUMENTS_READ`, `INCOME_DOCUMENTS_WRITE`, `OUTCOME_DOCUMENTS_READ`, `OUTCOME_DOCUMENTS_WRITE`, `USERS_READ`, `USERS_WRITE`, `ROLES_READ`, `ROLES_WRITE`.

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

### Склады (`/warehouses`)

Права: `WAREHOUSES_READ` / `WAREHOUSES_WRITE` / `FULL_ACCESS`. Системную запись нельзя править как обычную.

| Метод | Путь | Описание |
|---|---|---|
| GET | `/warehouses/` | Список |
| GET | `/warehouses/{id}` | По id |
| POST | `/warehouses/` | Создать `{ "name", "address" }` |
| PATCH | `/warehouses/{id}` | Изменить |
| PATCH | `/warehouses/{id}/mark-deleted` | Мягкое удаление |
| PATCH | `/warehouses/{id}/unmark-deleted` | Снять пометку |
| DELETE | `/warehouses/{id}` | Удалить |

Список складов в UI: `#/warehouses`. Раздел «Склад» (`#/warehouse`) — хаб документов, не этот справочник.

### Номенклатура (`/products`)

Права: `PRODUCTS_READ` / `PRODUCTS_WRITE` / `FULL_ACCESS`. Единица измерения — внешний ключ, удаление занятой единицы запрещено (`RESTRICT`).

| Метод | Путь | Описание |
|---|---|---|
| GET | `/products/` | Страница, по умолчанию 20. Фильтры: `name`, `article` |
| GET | `/products/{id}` | По id |
| POST | `/products/` | Создать `{ "name", "article", "unitId" }` |
| PATCH | `/products/{id}` | Изменить |
| PATCH | `/products/{id}/mark-deleted` | Мягкое удаление |
| PATCH | `/products/{id}/unmark-deleted` | Снять пометку |
| DELETE | `/products/{id}` | Удалить |

### Поступление (`/income-documents`)

Права: `INCOME_DOCUMENTS_READ` / `INCOME_DOCUMENTS_WRITE` / `FULL_ACCESS`.

Шапка не содержит строк. `PATCH` шапки меняет склад и дату и не перезаписывает табличную часть. Номер (`000001`, …) только для чтения: он записывается после вставки как шесть цифр id и уникален в своей таблице, включая помеченные документы. Количество в строке — больше нуля, до трёх знаков после запятой.

| Метод | Путь | Описание |
|---|---|---|
| GET | `/income-documents/` | Журнал, страница 20, сортировка по дате и id. Строк в списке нет |
| GET | `/income-documents/{id}` | Шапка и `lines[]` |
| POST | `/income-documents/` | Создать шапку `{ "warehouseId", "documentDate" }` |
| PATCH | `/income-documents/{id}` | Изменить шапку |
| PATCH | `/income-documents/{id}/mark-deleted` | Мягкое удаление |
| PATCH | `/income-documents/{id}/unmark-deleted` | Снять пометку |
| DELETE | `/income-documents/{id}` | Удалить |
| POST | `/income-documents/{id}/lines` | Добавить строку `{ "productId", "quantity" }` |
| PATCH | `/income-documents/{id}/lines/{lineId}` | Изменить строку |
| DELETE | `/income-documents/{id}/lines/{lineId}` | Удалить строку |

Пока шапка не записана, строки с клиента не отправляются. UI: `#/income-documents`.

### Реализация (`/outcome-documents`)

Зеркало поступления. Права: `OUTCOME_DOCUMENTS_READ` / `OUTCOME_DOCUMENTS_WRITE` / `FULL_ACCESS`. Номер уникален в таблице расхода, не в таблице прихода. Стартовый документ `000001` — свой.

Те же методы, что у `/income-documents`, с префиксом `/outcome-documents`. Тело шапки: `{ "warehouseId", "documentDate" }`. Тело строки: `{ "productId", "quantity" }`. UI: `#/outcome-documents`.

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

Автотестов в репозитории пока нет: `test` отрабатывает без проверок API и БД. Артефакт: `build/libs/` после `bootJar` / `build`.

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
