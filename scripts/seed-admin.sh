#!/usr/bin/env bash
# Creates the first admin account. Admin/Coordinator accounts can't self-register
# (see backend's AuthService) — the only way in is the bootstrap-admin mechanism:
# set these two env vars, restart the backend once, then blank them out again.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -z "${1:-}" ] || [ -z "${2:-}" ]; then
  echo "Usage: $0 <admin-email> <admin-password>"
  exit 1
fi
EMAIL="$1"
PASSWORD="$2"
ENV_FILE=infra/docker/.env

if [ ! -f "$ENV_FILE" ]; then
  echo "No $ENV_FILE found — run scripts/dev-up.sh at least once first."
  exit 1
fi

sed -i.bak \
  -e "s#^BOOTSTRAP_ADMIN_EMAIL=.*#BOOTSTRAP_ADMIN_EMAIL=${EMAIL}#" \
  -e "s#^BOOTSTRAP_ADMIN_PASSWORD=.*#BOOTSTRAP_ADMIN_PASSWORD=${PASSWORD}#" \
  "$ENV_FILE" && rm -f "$ENV_FILE.bak"

echo "Restarting backend with bootstrap admin credentials set..."
docker compose -f infra/docker/docker-compose.yml -f infra/kafka/docker-compose.kafka.yml --env-file "$ENV_FILE" up -d --force-recreate backend

echo "Waiting for backend to come back up..."
for i in $(seq 1 30); do
  if curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; then break; fi
  sleep 2
done

echo "Verifying login..."
if curl -fsS -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\"}" | grep -q accessToken; then
  echo "Admin account ready: ${EMAIL}"
else
  echo "Could not confirm login — check 'docker compose logs backend' for details."
  exit 1
fi

echo "Blanking the bootstrap vars back out so a restart never recreates/overwrites this account..."
sed -i.bak \
  -e "s#^BOOTSTRAP_ADMIN_EMAIL=.*#BOOTSTRAP_ADMIN_EMAIL=#" \
  -e "s#^BOOTSTRAP_ADMIN_PASSWORD=.*#BOOTSTRAP_ADMIN_PASSWORD=#" \
  "$ENV_FILE" && rm -f "$ENV_FILE.bak"
docker compose -f infra/docker/docker-compose.yml -f infra/kafka/docker-compose.kafka.yml --env-file "$ENV_FILE" up -d --force-recreate backend
echo "Done."
