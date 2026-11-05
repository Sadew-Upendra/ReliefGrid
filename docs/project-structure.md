# ReliefGrid — Project Structure, Database, and GitHub Guide

## 1. Complete Project (Monorepo) Structure

One repository, one root, each deployable piece in its own top-level folder. This is the standard layout for a multi-service project on GitHub.

```
reliefgrid/
├── .github/
│   └── workflows/
│       ├── backend-ci.yml
│       ├── frontend-ci.yml
│       ├── ai-service-ci.yml
│       └── docker-build.yml
├── frontend/                      # Next.js app (Step 4)
│   ├── src/
│   │   ├── app/
│   │   ├── components/
│   │   └── lib/
│   ├── public/
│   ├── package.json
│   └── Dockerfile
├── backend/                       # Spring Boot API (Step 5)
│   ├── src/main/java/com/reliefgrid/
│   │   ├── common/
│   │   ├── user/
│   │   ├── auth/
│   │   ├── request/
│   │   ├── inventory/
│   │   ├── matching/
│   │   └── task/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/          # Flyway SQL, versioned (V1__, V2__...)
│   ├── src/test/java/com/reliefgrid/
│   ├── pom.xml
│   └── Dockerfile
├── ai-service/                    # Python FastAPI (Phase 2)
│   ├── app/
│   │   ├── main.py
│   │   ├── models/
│   │   └── routers/
│   ├── requirements.txt
│   └── Dockerfile
├── infra/                         # Steps 8–11
│   ├── docker/
│   │   └── docker-compose.yml     # full local stack: db, backend, frontend, ai, kafka
│   ├── k8s/
│   │   ├── base/                  # namespace, deployments, services (kustomize base)
│   │   └── overlays/
│   │       ├── staging/
│   │       └── production/
│   ├── terraform/
│   │   ├── modules/
│   │   │   ├── vpc/
│   │   │   ├── eks/
│   │   │   ├── rds/
│   │   │   └── msk/
│   │   ├── environments/
│   │   │   ├── staging/
│   │   │   └── production/
│   │   └── backend.tf             # remote state (S3 + DynamoDB lock)
│   ├── jenkins/
│   │   └── Jenkinsfile
│   └── grafana/
│       └── dashboards/
├── docs/                          # Steps 1–3
│   ├── architecture.md
│   ├── api-spec.yaml
│   ├── database-guide.md
│   ├── project-structure.md       # this file
│   └── adr/
├── scripts/                       # one-off helper scripts (seed data, backups)
├── .gitignore
├── .editorconfig
├── LICENSE
└── README.md                      # top-level: what the project is, how to run all of it
```

**Why one repo (monorepo) rather than four separate repos:** at your scale, one repo means one `git clone`, one place for issues and PRs, and CI that can build only what changed. Each folder still has its own `Dockerfile`, its own dependency file, and its own CI workflow, so it stays independently buildable and deployable — splitting into separate repos later, if the project outgrows this, is a straightforward move.

---

## 2. Database Creation and Design Guide

### 2.1 Design principles used in ReliefGrid
1. **One database, one schema, per environment.** Local, staging, and production are separate PostgreSQL instances — never separate schemas in one instance for different environments.
2. **UUID primary keys**, not auto-increment integers. Safer to expose in URLs, and they merge cleanly if data ever moves between systems.
3. **Every table has `created_at`**; tables whose rows change state also have `updated_at`.
4. **Enums as `VARCHAR` with application-level enums**, not native Postgres `ENUM` types. Easier to extend (adding a new status is a data migration, not a schema-altering `ALTER TYPE`).
5. **Foreign keys enforced at the database**, even though the app also validates. The database is the last line of defense against bad data.
6. **Migrations, never manual schema edits.** Every schema change is a numbered Flyway file, committed to git, and applied the same way in every environment.
7. **Index what you filter and sort by.** ReliefGrid's requests are filtered by `status`, `urgency`, `reporter_id`, and sorted by `created_at` — all indexed.

### 2.2 Creating the database

**Local (via Docker, already in `infra/docker/docker-compose.yml`):**
```bash
docker compose up -d db
# Creates a `reliefgrid` database, user `reliefgrid`, with the PostGIS extension available.
```

**Manually, on any PostgreSQL 16+ server:**
```sql
CREATE DATABASE reliefgrid;
CREATE USER reliefgrid WITH ENCRYPTED PASSWORD 'change-me';
GRANT ALL PRIVILEGES ON DATABASE reliefgrid TO reliefgrid;
\c reliefgrid
CREATE EXTENSION IF NOT EXISTS postgis;
```

**Cloud (AWS RDS, for Step 11):**
- Engine: PostgreSQL 17, with the `postgis` extension enabled in the RDS parameter group
- Multi-AZ for production, single-AZ for staging
- Automated backups (7-day retention minimum), encryption at rest with KMS
- Never expose the instance publicly — it lives in a private subnet, reachable only from the EKS cluster's security group

### 2.3 Running migrations
Flyway runs automatically when the backend starts (`spring.flyway` is on by default once the dependency is present). Migration files live in `backend/src/main/resources/db/migration/`, named `V<number>__description.sql`:
```
V1__init.sql               # already created — see Step 5
V2__add_notifications.sql  # example future migration
```
Rules: never edit a migration that has already run anywhere (including your own machine); always add a new one. Never rename a migration file after it has run.

### 2.4 Current schema (from Step 5)
| Table | Purpose | Key columns |
|---|---|---|
| `users` | All accounts | `email` (unique), `role`, `password_hash` |
| `relief_requests` | Reported needs | `status`, `urgency`, `lat`/`lng`, `reporter_id` (FK) |
| `supply_points` | NGO/donor locations | `owner_id` (FK) |
| `inventory_items` | Stock per supply point | `supply_point_id` (FK), `need_type`, unique per (point, type) |
| `tasks` | Volunteer assignments | `request_id` (FK), `volunteer_id` (FK, nullable), `version` (optimistic lock) |
| `audit_logs` | Immutable action trail | `actor_id`, `action`, `entity_type`, `entity_id` |

See `docs/architecture.md` for the full ER diagram.

### 2.5 Backups and safety
- Automated daily snapshots in RDS; test a restore at least once before relying on it
- Never run destructive SQL directly against production — write a migration, test it in staging first
- Use a read replica for analytics/reporting once traffic justifies it, so heavy queries don't slow down the live app

---

## 3. GitHub Repository Guide

### 3.1 Create the repository
```bash
cd reliefgrid
git init
git add .
git commit -m "chore: initial project scaffold"
gh repo create reliefgrid --private --source=. --remote=origin
git push -u origin main
```
(Or create it on github.com first, then `git remote add origin <url>`.)

### 3.2 Branching model
Keep it simple for a solo/small-team learning project — **trunk-based with short-lived feature branches**:
```
main                 → always deployable; protected
feature/<name>       → e.g. feature/request-status-workflow
fix/<name>           → e.g. fix/task-double-accept
```
Avoid long-lived `develop`/`release` branches unless the team grows — they add merge overhead you don't need yet.

### 3.3 Branch protection (Settings → Branches → `main`)
- Require a pull request before merging (no direct pushes)
- Require status checks to pass: the CI workflows from Step 7
- Require at least 1 approval once you're not the only contributor
- Require branches to be up to date before merging
- Block force pushes to `main`

### 3.4 Commit messages — Conventional Commits
```
feat(request): add urgency-based sorting to request list
fix(task): prevent double-accept with optimistic locking
docs(readme): add local setup steps
chore(deps): bump spring-boot to 4.0.6
```
This makes the history readable and lets tools auto-generate changelogs later.

### 3.5 Issues and project board
- Use GitHub Issues with labels: `bug`, `feature`, `security`, `infra`, `good-first-issue`
- One GitHub Project (board) with columns: Backlog → In Progress → In Review → Done
- Reference issues in commits and PRs: `Closes #12`

### 3.6 Pull request template
`.github/PULL_REQUEST_TEMPLATE.md`:
```markdown
## What changed
## Why
## How to test
## Checklist
- [ ] Tests added/updated
- [ ] Docs updated if needed
- [ ] No secrets committed
```

### 3.7 Secrets — never in git
- Local: `.env` files, already in `.gitignore`
- CI: GitHub Actions → Settings → Secrets and variables → Actions (used in Step 7)
- Cloud: AWS Secrets Manager or SSM Parameter Store, referenced by Kubernetes/Terraform, never hardcoded

### 3.8 Repository settings worth turning on
- Dependabot alerts + security updates (Settings → Code security)
- Secret scanning and push protection
- Require signed commits, optional but a nice touch for a security-focused portfolio project

### 3.9 Suggested root files
- `README.md` — what ReliefGrid is, quick start for all three services, links to `docs/`
- `LICENSE` — MIT is fine for a portfolio project
- `.editorconfig` — consistent indentation across Java/TS/Python
- `CONTRIBUTING.md` — optional, useful if you want it to look open-source-ready
