# ReliefGrid API

Spring Boot 4 · Java 25 · PostgreSQL 17 + PostGIS · Flyway · JWT (Nimbus)

## Run locally
Requires JDK 25 and Maven 3.9+.
```bash
docker compose up -d db
export BOOTSTRAP_ADMIN_EMAIL=admin@reliefgrid.local
export BOOTSTRAP_ADMIN_PASSWORD='ChangeMe-12345'
mvn spring-boot:run          # http://localhost:8080
mvn test
```
Add the Maven wrapper once with `mvn wrapper:wrapper`.

## Structure (package by feature)
```
src/main/java/com/reliefgrid
├── common/     config, security (JWT), exception (ProblemDetail), audit, web
├── user/       User, Role, repository, admin bootstrap
├── auth/       register, login, refresh
├── request/    relief requests: create, search, status workflow
├── inventory/  supply points and stock
├── matching/   nearest supply (PostGIS)
└── task/       volunteer tasks
src/main/resources
├── application.yml
└── db/migration/V1__init.sql
```

## Endpoints (`/api/v1`)
| Method | Path | Who |
|---|---|---|
| POST | /auth/register, /auth/login, /auth/refresh, /auth/logout | public (rate-limited: 10/min/IP on register, login, refresh) |
| POST | /requests | any signed-in user |
| GET | /requests?status&needType&urgency&bbox&page&size | signed-in (citizens see only their own) |
| GET | /requests/{id} | signed-in |
| PATCH | /requests/{id}/status | coordinator, admin |
| GET | /requests/{id}/matches | coordinator, admin |
| POST | /requests/{id}/tasks | coordinator, admin |
| GET | /tasks?mine=true | volunteer, coordinator, admin |
| POST | /tasks/{id}/accept, /tasks/{id}/complete | volunteer |
| GET, POST | /supply-points | signed-in / NGO, admin |
| PUT | /supply-points/{id}/inventory | owner NGO, admin |
| GET | /audit-logs | admin |
| GET | /notifications | signed-in (always scoped to the caller) |
| GET | /requests/daily-counts?days=30 | coordinator, admin (feeds `ai-service`'s `/forecast` — see `docs/forecasting.md`) |
| GET | /public/status | public, no auth — aggregate counts only, backs the frontend's public `/status` page |

Health: `/actuator/health` · Metrics: `/actuator/prometheus`

## Security
Refresh tokens are opaque, hashed, and rotate on every use (reuse triggers full session revocation). Rate limiting, security headers, and dependency scanning are also in place. See `docs/security.md` for the full picture, and copy `.env.example` to `.env` before running.

## Notifications
Status changes and task assignments trigger an email via `NotificationService` — but only if `SMTP_HOST` is set; otherwise it logs what it would have sent (safe default for local dev and CI) and still records the notification in the database. See `docs/kafka.md`.
