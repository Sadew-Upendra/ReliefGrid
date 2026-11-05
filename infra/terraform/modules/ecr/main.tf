# Optional AWS-native alternative to GHCR (Step 7's default). Same repos either way —
# switching just means changing the registry host in docker-build.yml and the image
# names in infra/k8s/overlays/*/kustomization.yaml.

resource "aws_ecr_repository" "backend" {
  name                 = "${var.name}-backend"
  image_tag_mutability = "IMMUTABLE"
  image_scanning_configuration { scan_on_push = true }
}

resource "aws_ecr_repository" "frontend" {
  name                 = "${var.name}-frontend"
  image_tag_mutability = "IMMUTABLE"
  image_scanning_configuration { scan_on_push = true }
}

resource "aws_ecr_lifecycle_policy" "expire_untagged" {
  for_each   = { backend = aws_ecr_repository.backend.name, frontend = aws_ecr_repository.frontend.name }
  repository = each.value
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Expire untagged images after 14 days"
      selection    = { tagStatus = "untagged", countType = "sinceImagePushed", countUnit = "days", countNumber = 14 }
      action       = { type = "expire" }
    }]
  })
}
