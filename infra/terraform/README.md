# ReliefGrid — Terraform (AWS)

```
infra/terraform/
├── modules/
│   ├── vpc/    # VPC, public+private subnets, NAT gateway(s)
│   ├── eks/    # EKS cluster, managed node group, OIDC provider (for IRSA)
│   ├── rds/    # PostgreSQL (PostGIS-ready), password generated and stored in Secrets Manager — production only
│   ├── msk/    # MSK Serverless Kafka cluster, IAM-authenticated
│   ├── ecr/    # optional AWS-native alternative to GHCR for container images
│   ├── dns/    # Route 53 hosted zone (records themselves are managed by external-dns, in-cluster)
│   └── irsa/   # reusable "IAM role assumable by a given k8s ServiceAccount" module
└── environments/
    ├── staging/      # 1 NAT, small instances, NO RDS (uses in-cluster Postgres — see infra/k8s)
    └── production/   # 1 NAT per AZ, larger instances, Multi-AZ RDS, deletion protection
```

## Bootstrapping remote state (one-time, per AWS account)
Terraform can't create the bucket it stores its own state in, so this is done once, manually, before the first `terraform init`:
```bash
aws s3api create-bucket --bucket reliefgrid-terraform-state --region us-east-1
aws s3api put-bucket-versioning --bucket reliefgrid-terraform-state \
  --versioning-configuration Status=Enabled
aws dynamodb create-table --table-name reliefgrid-terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST
```

## Usage
```bash
cd infra/terraform/environments/staging   # or production
cp terraform.tfvars.example terraform.tfvars   # set your real domain_name
terraform init
terraform plan
terraform apply
```

## After `apply`
```bash
aws eks update-kubeconfig --name $(terraform output -raw cluster_name)
kubectl apply -k ../../k8s/overlays/staging   # or overlays/production
```
The database password is in Secrets Manager, not in Terraform output or state you'd paste anywhere:
```bash
aws secretsmanager get-secret-value \
  --secret-id $(terraform output -raw db_secret_arn) --query SecretString --output text
```
Use that, plus `terraform output db_endpoint`, to build the `reliefgrid-secrets` Kubernetes Secret described in `infra/k8s/README.md` — or better, point External Secrets Operator at this same Secrets Manager entry so it's never typed by hand.

## Notes
- **Why staging has no RDS module:** staging runs its own disposable Postgres inside the cluster (`infra/k8s/overlays/staging/postgres.yaml`) — cheaper and fine to lose. Production uses RDS because that data actually matters.
- **Why MSK Serverless over provisioned MSK:** no broker count/size to plan, billed by throughput — a better fit while traffic is unpredictable. Switch to `aws_msk_cluster` later if usage becomes steady enough that provisioned capacity is cheaper.
- **Why one shared EKS "cluster security group" for RDS/MSK access:** every managed node automatically belongs to it, so there's no separate node security group to keep in sync as the node group changes.
- **GHCR vs. ECR:** Step 7's CI pushes to GHCR by default — simplest, and free for public images. The `ecr` module exists if you'd rather keep images inside AWS (e.g. for a private VPC with no internet egress to ghcr.io): point `docker-build.yml`'s `REGISTRY`/image names at `terraform output ecr_backend_repository_url` / `ecr_frontend_repository_url`, log in with `aws ecr get-login-password`, and update the `images:` block in each `infra/k8s/overlays/*/kustomization.yaml` to match.
- **DNS:** `dns` creates the Route 53 hosted zone only. Actual records for `reliefgrid.example.com` / `api.reliefgrid.example.com` are created automatically by `external-dns`, running in-cluster and watching the `Ingress` object — see `infra/k8s/README.md`. After the first `apply`, point your domain registrar's nameservers at `terraform output route53_name_servers`.
- **IRSA:** the `irsa` module is a small, reusable "let this Kubernetes ServiceAccount assume this IAM role" building block, used for both `external-secrets` (reads Secrets Manager) and `external-dns` (writes Route 53).
- **Destroying an environment:** `terraform destroy` in the environment folder. Production has `deletion_protection = true` on RDS — remove that first (via a plan/apply) if you genuinely mean to destroy it.

## See also
`docs/deployment-aws.md` — the full, ordered runbook from an empty AWS account to a live app, tying this together with `infra/k8s` and the CI/CD pipeline.
