output "cluster_name" { value = module.eks.cluster_name }
output "cluster_endpoint" { value = module.eks.cluster_endpoint }
output "oidc_provider_arn" { value = module.eks.oidc_provider_arn }
output "app_secret_arn" { value = aws_secretsmanager_secret.app.arn }
output "kafka_bootstrap_brokers" { value = module.msk.bootstrap_brokers_sasl_iam }
output "ecr_backend_repository_url" { value = module.ecr.backend_repository_url }
output "ecr_frontend_repository_url" { value = module.ecr.frontend_repository_url }
output "route53_zone_id" { value = module.dns.zone_id }
output "route53_name_servers" { value = module.dns.name_servers }
output "external_secrets_role_arn" { value = module.external_secrets_irsa.role_arn }
output "external_dns_role_arn" { value = module.external_dns_irsa.role_arn }
# No db_endpoint/db_secret_arn here — staging has no RDS. See infra/k8s/overlays/staging/postgres.yaml.
