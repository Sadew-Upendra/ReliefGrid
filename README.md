# ReliefGrid

> A disaster relief coordination platform that connects people in need with supplies and volunteers, in real time.

## The Problem
During floods, landslides and other disasters, help is wasted. Donations pile up in one place while another has none, volunteers don't know where to go, and needs are scattered across WhatsApp and Facebook posts.

## The Solution
ReliefGrid gives coordinators one trusted source of truth:

- **Report needs** (food, medicine, shelter, rescue) with location and urgency
- **Pledge supplies** and manage inventory at supply points
- **Volunteer** for tasks near you
- **Live map** of needs vs. available resources
- **Smart matching** of needs to the nearest supply and volunteers
- **AI triage** that classifies urgency and extracts location and need type from free text
- **Dashboards, audit logs and reports** for coordinators and admins

## User Roles
| Role | Can do |
|---|---|
| Citizen / Reporter | Submit and track needs |
| Volunteer | Browse, accept and complete tasks |
| NGO / Donor | Manage supply points, inventory and pledges |
| Coordinator | Verify reports, assign tasks, close requests |
| Admin | Manage users, view audit logs, configure system |

## Tech Stack
| Layer | Technology |
|---|---|
| Frontend | Next.js, TypeScript, Tailwind CSS, shadcn/ui, MapLibre GL |
| Backend | Spring Boot 3, Java 21, Spring Security, JPA |
| Database | PostgreSQL + PostGIS, Redis |
| Messaging | Apache Kafka |
| AI service | Python, FastAPI, scikit-learn / transformers |
| CI/CD | GitHub Actions (CI), Jenkins (CD) |
| Infrastructure | Docker, Kubernetes, Terraform |
| Monitoring | Prometheus, Grafana |
| Cloud | AWS (EKS, RDS, MSK, ECR, S3, Route 53) |

## Documentation
- [Architecture and data model](docs/architecture.md)
- [API specification (OpenAPI)](docs/api-spec.yaml)
- [Security notes](docs/security.md) · [Frontend authentication](docs/frontend-auth.md)
- [Kafka](docs/kafka.md) · [Demand forecasting](docs/forecasting.md)
- [AWS deployment runbook](docs/deployment-aws.md) · [Release guide](docs/release-guide.md) · [Running locally](docs/running-locally.md)
- Architecture Decision Records: [docs/adr](docs/adr)

## Repository Structure
```
reliefgrid/
├── frontend/        # Next.js app
├── backend/         # Spring Boot API
├── ai-service/      # FastAPI service
├── infra/           # docker/, terraform/, k8s/, jenkins/, kafka/, grafana/
├── scripts/         # dev-up.sh, dev-down.sh, seed-admin.sh
├── docs/
└── CHANGELOG.md
```

## Getting Started

```bash
git clone https://github.com/Sadew-Upendra/reliefgrid.git
cd reliefgrid
./scripts/dev-up.sh
```
Starts the whole stack (database, backend, frontend, Kafka, AI service) with a freshly
generated JWT secret. Frontend at http://localhost:3000, API at http://localhost:8080.
Full walkthrough, including creating an admin account and verifying everything actually
works end to end: **[docs/running-locally.md](docs/running-locally.md)**.

Cutting and deploying a release: **[docs/release-guide.md](docs/release-guide.md)**. Change
history: **[CHANGELOG.md](CHANGELOG.md)**.

## Roadmap
- [x] **Phase 1 (MVP):** auth + RBAC, needs, supply and inventory, tasks, live map, nearest-match, audit log
- [x] **Phase 2:** Kafka streaming, AI triage service (`ai-service/`), notifications, Grafana dashboards
- [x] **Phase 3:** demand forecasting (API + coordinator chart at `/forecast`), offline-friendly PWA, multi-language (English, Sinhala, Tamil), public status page (`/status`)

## License
MIT
