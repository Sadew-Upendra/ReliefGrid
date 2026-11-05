#!/usr/bin/env bash
# Stops the local stack. Pass --volumes to also wipe the database (fresh start).
set -euo pipefail
cd "$(dirname "$0")/.."

ARGS=(-f infra/docker/docker-compose.yml -f infra/kafka/docker-compose.kafka.yml -f infra/grafana/docker-compose.monitoring.yml)
if [[ "${1:-}" == "--volumes" ]]; then
  echo "Stopping and wiping all data (database volume included)..."
  docker compose "${ARGS[@]}" down -v
else
  echo "Stopping (database volume preserved — use --volumes to wipe it)..."
  docker compose "${ARGS[@]}" down
fi
