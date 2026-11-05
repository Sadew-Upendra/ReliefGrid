# Remote state, isolated per environment by key. The bucket and lock table are
# created once, manually, before the first "terraform init" — see README.md
# in this folder ("Bootstrapping state").
terraform {
  backend "s3" {
    bucket         = "reliefgrid-terraform-state"
    key            = "staging/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "reliefgrid-terraform-locks"
    encrypt        = true
  }
}
