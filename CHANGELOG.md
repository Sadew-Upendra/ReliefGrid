# Changelog

All notable changes to ReliefGrid are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning follows
[Semantic Versioning](https://semver.org/) — see `docs/release-guide.md` for how a release
is actually cut.

## [Unreleased]
_Nothing yet — add entries here as you work, then move them under a new version heading
when you cut a release._

## [1.0.0] - 2026-11-06
Initial production release of the ReliefGrid monorepo, covering the complete architecture implementation (Spring Boot backend, Next.js frontend, FastAPI ML service, and cloud infrastructure).

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
- **Infrastructure**: Docker Compose (local dev & monitoring), Kubernetes manifests (kustomize base +
  staging/production overlays), Terraform for AWS (VPC, EKS, RDS, MSK Serverless, ECR,
  Route 53, IRSA for External Secrets and external-dns), Prometheus + Grafana setup
- **Docs**: architecture, ADRs, security, Kafka, forecasting, frontend auth, AWS deployment
  runbook, release guide, local running guide

### Fixed
- `listRequests()` on the frontend now correctly unwraps the backend's `PageResponse`
  wrapper instead of expecting a flat array

### Known Limitations
- Frontend session tokens are kept in `localStorage`, not an `httpOnly` cookie (tracked for future cookie-based auth migration).
- Sinhala and Tamil translations were generated with AI assistance and require native speaker verification.
- AWS MSK production IAM authentication profiles require final deployment-stage verification before production connection.
