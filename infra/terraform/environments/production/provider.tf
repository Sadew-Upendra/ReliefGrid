provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project     = "reliefgrid"
      Environment = "production"
      ManagedBy   = "terraform"
    }
  }
}
