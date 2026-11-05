locals {
  name = "reliefgrid-staging"
}

module "vpc" {
  source            = "../../modules/vpc"
  name              = local.name
  nat_gateway_count = 1 # cheaper for staging; production uses one per AZ
}

module "eks" {
  source              = "../../modules/eks"
  name                = local.name
  private_subnet_ids  = module.vpc.private_subnet_ids
  public_subnet_ids   = module.vpc.public_subnet_ids
  node_instance_types = ["t3.medium"]
  node_desired_size   = 2
  node_min_size       = 1
  node_max_size       = 3
}

# No RDS in staging on purpose: infra/k8s/overlays/staging/postgres.yaml runs a disposable
# in-cluster Postgres instead. Production uses RDS — see environments/production/main.tf.

module "msk" {
  source                      = "../../modules/msk"
  name                        = local.name
  vpc_id                      = module.vpc.vpc_id
  private_subnet_ids          = module.vpc.private_subnet_ids
  eks_node_security_group_id  = module.eks.cluster_security_group_id
}

module "ecr" {
  source = "../../modules/ecr"
  name   = local.name
}

module "dns" {
  source      = "../../modules/dns"
  domain_name = var.domain_name
}

# One JWT secret + Kafka bootstrap address + disposable Postgres credentials, consolidated so
# External Secrets can sync them straight into the "reliefgrid-secrets" k8s Secret — see
# infra/k8s/overlays/staging/external-secrets.yaml.
resource "random_password" "jwt_secret" {
  length  = 48
  special = false # keep it JWT/env-var safe; length alone gives well over 32 bytes of entropy
}

resource "random_password" "staging_db_password" {
  length  = 24
  special = false
}

resource "aws_secretsmanager_secret" "app" {
  name = "${local.name}-app-secrets"
}

resource "aws_secretsmanager_secret_version" "app" {
  secret_id = aws_secretsmanager_secret.app.id
  secret_string = jsonencode({
    jwt_secret               = random_password.jwt_secret.result
    kafka_bootstrap_servers  = module.msk.bootstrap_brokers_sasl_iam
    bootstrap_admin_email    = ""
    bootstrap_admin_password = ""
    db_user                  = "reliefgrid"
    db_password              = random_password.staging_db_password.result
  })
}

locals {
  oidc_issuer_no_scheme = replace(module.eks.oidc_issuer_url, "https://", "")
}

module "external_secrets_irsa" {
  source                = "../../modules/irsa"
  role_name             = "${local.name}-external-secrets"
  oidc_provider_arn     = module.eks.oidc_provider_arn
  oidc_provider_url     = local.oidc_issuer_no_scheme
  namespace             = "external-secrets"
  service_account_name  = "external-secrets"
  policy_json = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue", "secretsmanager:DescribeSecret"]
      Resource = [aws_secretsmanager_secret.app.arn]
    }]
  })
}

module "external_dns_irsa" {
  source               = "../../modules/irsa"
  role_name            = "${local.name}-external-dns"
  oidc_provider_arn    = module.eks.oidc_provider_arn
  oidc_provider_url    = local.oidc_issuer_no_scheme
  namespace            = "external-dns"
  service_account_name = "external-dns"
  policy_json = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["route53:ChangeResourceRecordSets"]
        Resource = ["arn:aws:route53:::hostedzone/${module.dns.zone_id}"]
      },
      {
        Effect   = "Allow"
        Action   = ["route53:ListHostedZones", "route53:ListResourceRecordSets"]
        Resource = ["*"]
      }
    ]
  })
}
