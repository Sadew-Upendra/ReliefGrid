# ReliefGrid — End-to-End AWS Deployment Runbook (Step 11)

This is the order of operations that ties together everything from Steps 7–10: Terraform,
Kubernetes, Docker images, and the CI/CD pipeline, into an actual running environment on AWS.
Do this once per environment (staging first, then production).

## 0. Prerequisites
- An AWS account, and an IAM user/role with enough permission to create the resources below (or use an org's existing deploy role)
- A domain you control (for Route 53 + TLS) — optional but assumed below
- Installed locally: `terraform` ≥ 1.9, `aws` CLI, `kubectl`, `kustomize`, `helm`

## 1. Bootstrap Terraform state (one-time, per AWS account)
See `infra/terraform/README.md` "Bootstrapping remote state" — creates the S3 bucket + DynamoDB lock table Terraform needs before it can manage anything else.

## 2. Provision AWS infrastructure
```bash
cd infra/terraform/environments/staging
cp terraform.tfvars.example terraform.tfvars   # set domain_name
terraform init
terraform apply
```
This creates: VPC, EKS cluster + node group, MSK Serverless, ECR repos, a Route 53 hosted zone, an app-secrets entry in Secrets Manager (JWT + Kafka + disposable DB creds for staging), and IAM roles for External Secrets and external-dns.

**Point your domain's nameservers** at `terraform output route53_name_servers` (skip if you don't own the domain yet — everything else still works via the load balancer's own hostname).

## 3. Connect kubectl to the new cluster
```bash
aws eks update-kubeconfig --name $(terraform output -raw cluster_name)
kubectl get nodes   # should list 1-2 nodes, Ready
```

## 4. Install cluster add-ons
Full commands in `infra/k8s/README.md` "Installing cluster add-ons": `external-secrets`, `external-dns`, `ingress-nginx`, `cert-manager`. Annotate each add-on's ServiceAccount with the matching Terraform output (`external_secrets_role_arn`, `external_dns_role_arn`).

## 5. Wire the ExternalSecret to the real IAM role
```bash
ARN=$(terraform output -raw external_secrets_role_arn)
sed -i "s#REPLACE_WITH_terraform_output_external_secrets_role_arn#$ARN#" \
  ../../../k8s/overlays/staging/external-secrets.yaml
```

## 6. Point the overlay at real images and your domain
```bash
cd ../../../k8s/overlays/staging
sed -i 's#ghcr.io/OWNER#ghcr.io/<your-github-username>#g' ../../base/*.yaml
sed -i 's#reliefgrid.example.com#staging.reliefgrid.yourdomain.com#g;
        s#api.reliefgrid.example.com#api-staging.reliefgrid.yourdomain.com#g' ../../base/ingress.yaml
```
(Do the equivalent in `overlays/production` with the production domain, later.)

## 7. Push an image once, manually, for the first deploy
Steps 1–10 already gave you GitHub Actions (`docker-build.yml`) — the normal path is: push to `main`, let CI build and push automatically. For the very first deploy, either wait for that to run once, or build and push by hand:
```bash
docker build -t ghcr.io/<you>/reliefgrid-backend:v0.1.0 ./backend
docker push ghcr.io/<you>/reliefgrid-backend:v0.1.0
```

## 8. Deploy the application
```bash
kubectl apply -k .   # from infra/k8s/overlays/staging
kubectl -n reliefgrid get pods -w
```
Once pods are `Running` and `1/1 Ready`, check:
```bash
kubectl -n external-dns logs deploy/external-dns   # should show a Route 53 record created
curl -k https://api-staging.reliefgrid.yourdomain.com/actuator/health
```
`cert-manager` takes a minute or two to issue the first TLS certificate — `-k` above skips verification only until that finishes.

## 9. Hand off to Jenkins for ongoing deploys
From here on, don't run `kubectl apply` by hand — let the pipeline do it (`infra/jenkins/Jenkinsfile`, Step 10): GitHub Actions builds and pushes on every merge to `main`, Jenkins deploys to staging automatically and to production after a manual approval.

## 10. Repeat for production
Same steps, `environments/production` and `overlays/production`. Production additionally has RDS (Multi-AZ, deletion-protected) instead of the in-cluster Postgres, and pinned image tags instead of `:latest` — see `docs/adr/` and `infra/terraform/README.md` for why.

## What you now have, end to end
```
git push → GitHub Actions (test, scan, build, push image)
         → Jenkins (deploy to staging → smoke test → approve → deploy to production)
Kubernetes (EKS) → NetworkPolicy-restricted backend, autoscaled frontend/backend
              → External Secrets syncing AWS Secrets Manager, no secret ever typed twice
              → external-dns keeping Route 53 in sync with the Ingress
              → cert-manager renewing TLS automatically
RDS (production) / in-cluster Postgres (staging) → the actual data
MSK → request lifecycle events, ready for the AI service (Phase 2) to consume
Prometheus + Grafana → dashboards on the running system (Step 10)
```

## Cost note
Staging is intentionally the cheap path: 1 NAT gateway, `t3.medium` nodes, `t3.micro`-class disposable Postgres, MSK Serverless (pay-per-use). This is still real AWS spend — mainly the EKS control plane (~$0.10/hr) and the NAT gateway. `terraform destroy` in `environments/staging` tears it all down when you're not actively working on it.
