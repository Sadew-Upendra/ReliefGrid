#!/usr/bin/env bash
# Starts the full ReliefGrid stack locally: Postgres, backend, frontend, Kafka, AI service.
# Monitoring (Prometheus/Grafana) is optional — pass --monitoring to include it.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f infra/docker/.env ]; then
  echo "No infra/docker/.env yet — creating one from .env.example with a freshly generated JWT_SECRET."
  cp infra/docker/.env.example infra/docker/.env
  SECRET=$(openssl rand -base64 48 | tr -d '\n')
  sed -i.bak "s#^JWT_SECRET=.*#JWT_SECRET=${SECRET}#" infra/docker/.env && rm -f infra/docker/.env.bak
  echo "Generated a random JWT_SECRET. This .env is for local dev only — never commit it (it's already .gitignored)."
fi

COMPOSE_FILES=(-f infra/docker/docker-compose.yml -f infra/kafka/docker-compose.kafka.yml)
if [[ "${1:-}" == "--monitoring" ]]; then
  COMPOSE_FILES+=(-f infra/grafana/docker-compose.monitoring.yml)
  echo "Including Prometheus + Grafana."
fi

echo "Building and starting: ${COMPOSE_FILES[*]}"
docker compose "${COMPOSE_FILES[@]}" --env-file infra/docker/.env up --build -d

echo
echo "Waiting for the backend to report healthy..."
for i in $(seq 1 30); do
  if curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo "Backend is up."
    break
  fi
  sleep 2
done

cat <<'MSG'

Stack is up:
  Frontend:    http://localhost:3000
  Backend:     http://localhost:8080  (health: /actuator/health)
  AI service:  http://localhost:8000  (health: /health)
  Postgres:    localhost:5432
  Kafka:       localhost:9092

Next: run scripts/seed-admin.sh to create a first admin account, or register
normally through the frontend at http://localhost:3000.

Logs:   docker compose -f infra/docker/docker-compose.yml -f infra/kafka/docker-compose.kafka.yml logs -f
Stop:   scripts/dev-down.sh
MSG
