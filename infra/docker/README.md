# Local full-stack Docker setup

Runs the database, backend API, and frontend together — no local JDK or Node install needed.

```bash
cp infra/docker/.env.example infra/docker/.env
# edit infra/docker/.env — at minimum set JWT_SECRET

docker compose -f infra/docker/docker-compose.yml --env-file infra/docker/.env up --build
```

- Frontend: http://localhost:3000
- Backend: http://localhost:8080 (health: `/actuator/health`)
- Database: localhost:5432

To create the first admin account, set `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` in `.env` before the first `up`, then blank them out and restart — the backend only creates that account when no admin exists yet.

Stop and remove containers (keeps the database volume):
```bash
docker compose -f infra/docker/docker-compose.yml down
```
Stop and wipe all data:
```bash
docker compose -f infra/docker/docker-compose.yml down -v
```

For backend-only development (faster inner loop, no image rebuild per code change), use `backend/docker-compose.yml` instead, which starts just the database, and run the API with `mvn spring-boot:run`.
