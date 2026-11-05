# The hosted zone only. Individual records (reliefgrid.example.com, api.reliefgrid.example.com)
# are NOT created here — they're created automatically by external-dns, running in-cluster,
# watching Ingress objects and syncing Route 53 to match. That avoids the chicken-and-egg
# problem of Terraform needing a load balancer hostname that doesn't exist until after
# ingress-nginx is installed into the cluster.

resource "aws_route53_zone" "this" {
  name = var.domain_name
}
