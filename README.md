# Subtrack API

Backend REST API for **Subtrack**, a SaaS subscription tracking and cost management application. Built with Spring
Boot, PostgreSQL, JPA/Hibernate, and Flyway.

## Key Features

- User registration and login (BCrypt-hashed passwords)
- CRUD for subscriptions (create, update, soft delete, list)
- Automatic change history tracking for every subscription create/update/delete
- Soft deletion for both users and subscriptions (no hard deletes)
- Centralized error handling with consistent JSON error responses
- Database schema managed by Flyway migrations

## Requirements

- Java 21+
- Maven (or the bundled `mvnw` / `mvnw.cmd` wrapper)
- Docker (to run PostgreSQL via `docker-compose.yml`)

## How to Run

1. Start PostgreSQL:
   ```
   cd subtrack-api
   docker-compose up -d
   ```
2. Run the application:
   ```
   ./mvnw spring-boot:run
   ```
   The API listens on `http://localhost:8080`.

Flyway automatically creates the schema (`V1__init_tables.sql`) and seeds demo data (`V2__insert_dummy_data.sql`) on
startup. The seeded demo users (alice@example.com, bob@example.com, carol@example.com, dave@example.com,
eve@example.com) all share the password `Password123!`.

## How to Build

```
./mvnw clean package
```

## API Overview

All endpoints are prefixed with `/api`.

| Method | Path                     | Description                                              |
|--------|--------------------------|-----------------------------------------------------------|
| POST   | `/users`                 | Register a new user                                       |
| POST   | `/users/login`           | Authenticate with email/password                           |
| GET    | `/users/{id}`            | Get an active user by id                                   |
| DELETE | `/users/{id}`            | Soft delete a user                                         |
| GET    | `/subscriptions`         | List active subscriptions (requires `X-User-Id`)           |
| POST   | `/subscriptions`         | Create a subscription (requires `X-User-Id`)                |
| PUT    | `/subscriptions/{id}`    | Update a subscription (requires `X-User-Id`)                |
| DELETE | `/subscriptions/{id}`    | Soft delete a subscription (requires `X-User-Id`)           |
| GET    | `/subscriptions/history` | List subscription change history (requires `X-User-Id`)    |

`X-User-Id` identifies the acting user; it is issued to the client after a successful login via `/users/login`.

## Directory Structure

```
subtrack-api/
  src/main/java/io/github/kitamura/subtrack_api/
    controller/   REST controllers
    service/      Business logic
    entity/       JPA entities
    dto/          Request/response payloads
    mapper/       Entity <-> DTO mapping
    repository/   Spring Data JPA repositories
    exception/    Centralized exception handling
    config/       Security configuration
  src/main/resources/
    application.yml      Spring/DB/Flyway configuration
    db/migration/         Flyway SQL migrations
  docker-compose.yml       Local PostgreSQL instance
```

## Notes on Security

This project is a portfolio/demo application. Authentication uses a simple email/password login
(`POST /users/login`) that returns the user id, which the frontend then sends via the `X-User-Id` header on
subsequent requests. There is no session/JWT layer, so this should not be used as-is in production without adding
proper token-based authentication and authorization.

## License

This project is released under the MIT License.
