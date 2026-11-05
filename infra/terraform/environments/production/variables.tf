variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "domain_name" {
  description = "Domain to manage in Route 53 for this environment, e.g. staging.reliefgrid.example.com"
  type        = string
}
