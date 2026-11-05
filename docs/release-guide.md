# ReliefGrid — Release Guide

## Versioning
Semantic versioning (`vMAJOR.MINOR.PATCH`), one version number shared across backend,
frontend, and AI service — they deploy together as one release, even when only one of them
actually changed, so "what's running in production" is always a single, unambiguous tag.

- **MAJOR** — breaking API change (a field removed/renamed in a response, an endpoint removed) or a schema change that isn't backward-compatible with the previous release running during a rolling deploy
- **MINOR** — new feature, new endpoint, additive schema change (new column/table)
- **PATCH** — bug fix, security patch, dependency bump, no behavior change for API consumers

## Before cutting a release
Run through this on `main` before tagging — catching a problem here is far cheaper than
catching it in production:

- [ ] `mvn test` passes (backend) — includes the JWT, refresh-token, and notification tests
- [ ] `pytest` passes (ai-service) — includes classifier and forecaster tests
- [ ] `npx tsc --noEmit` and `npm run build` pass (frontend)
- [ ] `mvn org.owasp:dependency-check-maven:check` has no new CVE ≥ CVSS 8 (or it's in `backend/owasp-suppressions.xml` with a reviewed justification)
- [ ] Any new Flyway migration (`backend/src/main/resources/db/migration/V*.sql`) has been tested against a copy of staging's data, not just a fresh database
- [ ] `CHANGELOG.md`'s `[Unreleased]` section is accurate and ready to become the new version's entry
- [ ] If this release touches Terraform (`infra/terraform`), `terraform plan` has been reviewed for both `staging` and `production` — a release should never surprise you at `apply` time

## Cutting the release
```bash
# 1. Move CHANGELOG.md's [Unreleased] entries under a new ## [vX.Y.Z] - YYYY-MM-DD heading
git add CHANGELOG.md
git commit -m "chore(release): vX.Y.Z"

# 2. Tag and push
git tag -a vX.Y.Z -m "vX.Y.Z"
git push origin main --tags
```
Pushing the tag triggers `docker-build.yml` (Step 7), which — after `backend-ci`,
`frontend-ci`, and `ai-service-ci` all pass on that commit — builds and pushes all three
images to GHCR tagged both `vX.Y.Z` and with the commit SHA.

## Deploying the release
Handled by `infra/jenkins/Jenkinsfile` (Step 10), not by hand:
1. Jenkins deploys `vX.Y.Z` to **staging** automatically once the images are published, then runs its smoke test.
2. Pushed to a **manual approval gate** — someone reviews staging, then approves.
3. Jenkins deploys the same `vX.Y.Z` to **production**, then smoke-tests it.

To deploy by hand instead (e.g. no Jenkins set up yet):
```bash
cd infra/k8s/overlays/production
kustomize edit set image ghcr.io/<you>/reliefgrid-backend=ghcr.io/<you>/reliefgrid-backend:vX.Y.Z
kustomize edit set image ghcr.io/<you>/reliefgrid-frontend=ghcr.io/<you>/reliefgrid-frontend:vX.Y.Z
kustomize edit set image ghcr.io/<you>/reliefgrid-ai-service=ghcr.io/<you>/reliefgrid-ai-service:vX.Y.Z
kubectl apply -k .
kubectl -n reliefgrid rollout status deployment/backend
kubectl -n reliefgrid rollout status deployment/frontend
kubectl -n reliefgrid rollout status deployment/ai-service
```
Production's `kustomization.yaml` pins real tags on purpose (never `:latest`) — see
`infra/terraform/README.md` — so this `kustomize edit` is always explicit about which
version is going out.

## Rolling back
Kubernetes keeps previous ReplicaSets, so a bad deploy is a one-line fix:
```bash
kubectl -n reliefgrid rollout undo deployment/backend
kubectl -n reliefgrid rollout undo deployment/frontend
kubectl -n reliefgrid rollout undo deployment/ai-service
```
For a database migration that shipped with the bad release: **do not** try to reverse a
Flyway migration by editing or deleting it. Write a new migration that undoes the specific
change, test it the same way as any other migration, and ship it as a patch release.

## Hotfixes
A critical bug in production doesn't wait for the next planned release:
```bash
git checkout -b fix/short-description main
# fix, commit, PR, merge to main as usual (Step 7's CI still gates it)
```
Then cut a PATCH release through the normal process above. There is no separate
hotfix/long-lived-branch process — `main` is always the next release candidate (see
`docs/project-structure.md` §3.2's trunk-based branching).

## Where "what's currently deployed" lives
- `kubectl -n reliefgrid get deployment backend -o jsonpath='{.spec.template.spec.containers[0].image}'` — the actual running image tag, source of truth
- GitHub's Releases page (create one from each `vX.Y.Z` tag, pasting that version's `CHANGELOG.md` section) — the human-readable record
- Grafana (Step 10) — confirms the new version is actually healthy post-deploy, not just "deployed"
