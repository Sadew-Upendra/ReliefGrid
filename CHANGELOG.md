# Changelog

All notable changes to ReliefGrid are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning follows
[Semantic Versioning](https://semver.org/) — see `docs/release-guide.md` for how a release
is actually cut.

## [Unreleased]
_Nothing yet — add entries here as you work, then move them under a new version heading
when you cut a release._

## [0.1.0]
Initial build, covering the full original roadmap (Phases 1–3) plus the structural
follow-ups identified along the way.

### Added
- **Backend**: Spring Boot 4 / Java 21 API — auth (JWT access + rotating refresh tokens),
  relief requests with a verified status workflow, supply points and inventory, volunteer
  tasks, PostGIS nearest-match, audit log, notifications, public aggregate status endpoint,
  demand-forecasting data endpoint
- **Frontend**: Next.js app — live map, request reporting, coordinator moderation, real
  login/register wired to the backend, role-based UI gating, a public `/status` page, a
  coordinator-only `/forecast` chart, multi-language (English, Sinhala, Tamil), basic
  offline/PWA support
- **AI service**: FastAPI + scikit-learn — urgency/need-type classification from free text
  (consumes `request.created`, publishes `ai.classification-result`), demand forecasting
  (`/forecast`)
- **Security**: rate limiting, security headers, refresh-token rotation with reuse
  detection, OWASP dependency scanning, Dependabot
- **CI/CD**: GitHub Actions for backend/frontend/AI service, Docker image build and push,
  Jenkins pipeline for staged deployment with a manual production approval gate
- **Infrastructure**: Docker Compose (local), Kubernetes manifests (kustomize base +
  staging/production overlays), Terraform for AWS (VPC, EKS, RDS, MSK Serverless, ECR,
  Route 53, IRSA for External Secrets and external-dns), Prometheus + Grafana
- **Docs**: architecture, ADRs, security, Kafka, forecasting, frontend auth, AWS deployment
  runbook, this release guide, local running guide

### Fixed
- `listRequests()` on the frontend now correctly unwraps the backend's `PageResponse`
  wrapper instead of expecting a flat array (never caught earlier because the frontend had
  never actually been wired to the real backend with a valid session until auth was added)

### Known limitations (tracked, not hidden)
- Frontend session tokens are kept in `localStorage`, not an `httpOnly` cookie — fine for
  this prototype, not for a production deployment facing the public internet (see
  `docs/frontend-auth.md`)
- Sinhala and Tamil translations were written with AI assistance and haven't had a native
  speaker review yet
- Kafka's production wiring (`infra/terraform/modules/msk`) uses IAM auth, but the backend
  and AI service still need the IAM auth library and a production Spring/Python profile
  added before they can actually connect to MSK — see `docs/kafka.md`
