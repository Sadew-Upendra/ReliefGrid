# Running ReliefGrid Locally

Two ways to run it: the easy way (Docker, everything in containers) and the dev way
(each service run natively, for fast reload while coding). Start with the easy way to
confirm everything works, then switch to the dev way for whichever piece you're changing.

## Prerequisites
- Docker and Docker Compose v2 (`docker compose version`)
- For native dev: JDK 25, Node.js 24, Python 3.13, Maven 3.9+
- `curl` and `openssl` (used by the helper scripts)

## Option A — Everything in Docker (easiest)

```bash
./scripts/dev-up.sh              # add --monitoring for Prometheus + Grafana too
```

This copies `infra/docker/.env.example` to `infra/docker/.env` on first run (generating a
real random `JWT_SECRET` for you — never commit this file; it's already in `.gitignore`),
then starts Postgres, the backend, the frontend, Kafka, and the AI service, and waits for
the backend to report healthy.

```
Frontend:    http://localhost:3000
Backend:     http://localhost:8080   (health: /actuator/health)
AI service:  http://localhost:8000   (health: /health)
Postgres:    localhost:5432
Kafka:       localhost:9092
```

Create your first account at `http://localhost:3000/register` — self-registration gives you
CITIZEN, VOLUNTEER, or NGO. For an admin or coordinator account (needed to see the full
dashboard moderation controls and `/forecast`):

```bash
./scripts/seed-admin.sh admin@reliefgrid.com 'admin@reliefgrid123'
```

This temporarily sets the bootstrap-admin env vars, restarts just the backend, confirms the
account can log in, then blanks the vars out again and restarts once more — so re-running
`dev-up.sh` later never silently recreates or overwrites that account.

Logs:
```bash
docker compose -f infra/docker/docker-compose.yml -f infra/kafka/docker-compose.kafka.yml logs -f
docker compose -f infra/docker/docker-compose.yml logs -f backend   # just one service
```

Stop:
```bash
./scripts/dev-down.sh            # keeps the database
./scripts/dev-down.sh --volumes  # wipes it too — fresh start next time
```

### Add monitoring
```bash
./scripts/dev-up.sh --monitoring
```
Grafana: `http://localhost:3001` (user `admin`, password `admin` — change it if you'll leave
this running). The "ReliefGrid Backend" dashboard is pre-provisioned under the ReliefGrid folder.

## Option B — Native dev (fast reload per service)

Run Postgres (and optionally Kafka) in Docker, everything else natively.

**Database only:**
```bash
cd backend && docker compose up -d db
```

**Backend:**
```bash
cd backend
cp .env.example .env   # edit as needed
export $(grep -v '^#' .env | xargs)   # or use an IDE run config with these vars
mvn spring-boot:run
```
Runs on `:8080`, reloading on rebuild (`mvn compile` in another terminal, or your IDE's
auto-build). Run `mvn test` here before trusting any change.

**Frontend:**
```bash
cd frontend
npm install
cp .env.example .env.local
npm run dev
```
Runs on `:3000` with hot reload. Leave `NEXT_PUBLIC_API_URL` blank in `.env.local` to use
built-in demo data instead of a real backend — useful for pure UI work.

**AI service:**
```bash
cd ai-service
python -m venv .venv && source .venv/bin/activate
pip install -r requirements-dev.txt
cp .env.example .env
uvicorn app.main:app --reload
```
Runs on `:8000`. Needs Kafka running to actually consume anything — see below.

**Kafka (needed for AI triage and notifications to actually fire):**
```bash
docker compose -f infra/docker/docker-compose.yml -f infra/kafka/docker-compose.kafka.yml up -d kafka kafka-topics-init
```

## Verifying it's actually working end to end
1. Register a CITIZEN account at `/register`, log in.
2. Submit a request from `/dashboard` with a description containing something like "trapped" —
   within a couple of seconds (via Kafka + the AI service), its urgency should update to
   CRITICAL even if you didn't check "life at risk".
3. Log in as the admin/coordinator account from `seed-admin.sh`, verify the request, then
   check `/forecast` loads (needs at least a little history to show anything interesting —
   demo mode fakes this; a fresh real database won't have much yet).
4. Check `/status` (no login needed) shows the aggregate counts.
5. Check the backend logs for a `[DEV: no SMTP configured] Would email ...` line after that
   verification — confirms the notification path fired.

## Common issues
| Symptom | Likely cause |
|---|---|
| Backend won't start: `app.jwt.secret must be at least 32 characters` | `.env`'s `JWT_SECRET` is empty or too short |
| Backend won't start: Flyway migration checksum mismatch | A migration file was edited after it already ran — never edit a shipped migration, add a new one |
| Frontend shows demo data even with a backend running | `NEXT_PUBLIC_API_URL` not set, or set after the dev server already started (Next.js reads it at build/start time) |
| AI classification never updates a request's urgency | AI service not running, or it can't reach Kafka — check `docker compose logs ai-service` |
| `403 Forbidden` on `/forecast` or `/audit-logs` | Logged in as a non-coordinator/admin account — expected, see `docs/frontend-auth.md` |
| `429 Too Many Requests` on login | Rate limiter (10/min/IP on auth endpoints, Step 6) — wait a minute |

See also: `backend/README.md`, `frontend/README.md`, `ai-service/README.md`, and
`infra/docker/README.md` for per-service detail this guide doesn't repeat.
