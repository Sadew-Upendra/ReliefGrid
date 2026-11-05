# ReliefGrid on Kubernetes

Layout follows kustomize's base + overlays pattern:
```
infra/k8s/
├── base/                  # shared manifests: Deployments, Services, HPAs, Ingress, NetworkPolicy
└── overlays/
    ├── staging/            # + in-cluster Postgres, 1 replica, staging.reliefgrid.example.com
    └── production/         # 3+ replicas, autoscaling to 10, RDS (no in-cluster DB), pinned image tags
```

## Secrets: automatic via Terraform + External Secrets (Step 11)
Each overlay now ships its own `external-secrets.yaml`: a `ServiceAccount` (IRSA-annotated),
`SecretStore`, and `ExternalSecret` that sync AWS Secrets Manager into a real
`reliefgrid-secrets` k8s Secret automatically, refreshed hourly. `base/secret.example.yaml` is
still a plain template, kept only as a fallback for quick local/kind testing with no AWS
account at all — it's not referenced by any `kustomization.yaml`, so `kubectl apply -k` never
deploys it on its own.

**Before applying an overlay for the first time**, replace its `external-secrets.yaml` placeholder with the real role ARN:
```bash
cd infra/terraform/environments/staging   # or production
ARN=$(terraform output -raw external_secrets_role_arn)
sed -i "s#REPLACE_WITH_terraform_output_external_secrets_role_arn#$ARN#" \
  ../../../k8s/overlays/staging/external-secrets.yaml
```
See `docs/deployment-aws.md` for the full, ordered runbook — Terraform apply, cluster add-ons, then this.

## Installing cluster add-ons (once per cluster, via Helm)
```bash
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets -n external-secrets --create-namespace

helm repo add external-dns https://kubernetes-sigs.github.io/external-dns/
helm install external-dns external-dns/external-dns -n external-dns --create-namespace \
  --set provider=aws --set policy=sync --set txtOwnerId=reliefgrid

helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm install ingress-nginx ingress-nginx/ingress-nginx -n ingress-nginx --create-namespace

helm repo add jetstack https://charts.jetstack.io
helm install cert-manager jetstack/cert-manager -n cert-manager --create-namespace --set crds.enabled=true
```
`external-dns` and `external-secrets`'s own ServiceAccounts also need the IRSA role ARNs from
Terraform (`external_dns_role_arn`, `external_secrets_role_arn`) — pass them via `--set
serviceAccount.annotations."eks\.amazonaws\.com/role-arn"=<arn>` on each `helm install`, or patch
the ServiceAccount afterward with `kubectl annotate`.

## Deploy

```bash
# Staging (includes its own throwaway Postgres)
kubectl apply -k infra/k8s/overlays/staging

# Production (expects reliefgrid-secrets to already point at RDS)
kubectl apply -k infra/k8s/overlays/production
```

Preview the rendered manifests first, without applying:
```bash
kubectl kustomize infra/k8s/overlays/staging
```

## What's included
- **Deployments** for backend and frontend, non-root, `allowPrivilegeEscalation: false`, all capabilities dropped
- **Liveness/readiness probes** against Spring Boot Actuator's `/actuator/health/{liveness,readiness}` groups (enabled in `application.yml`)
- **HorizontalPodAutoscaler** on CPU for both services
- **PodDisruptionBudget** for the backend, so a node drain can't take every replica down at once
- **NetworkPolicy** restricting who can reach the backend (ingress controller + monitoring namespace only)
- **Ingress** (nginx + cert-manager) routing `reliefgrid.example.com` → frontend and `api.reliefgrid.example.com` → backend, with TLS
- **External Secrets** wiring for automatic, no-typing secret sync from AWS Secrets Manager

## Before you deploy for real
Replace every `example.com` and `ghcr.io/OWNER/...` placeholder with your actual domain and GHCR (or ECR — see `infra/terraform/README.md`) registry path (`sed -i` across `infra/k8s` works fine).

