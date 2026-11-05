provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project     = "reliefgrid"
      Environment = "staging"
      ManagedBy   = "terraform"
    }
  }
}
