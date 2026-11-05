variable "name" {
  description = "Prefix for resource names, e.g. \"reliefgrid-staging\""
  type        = string
}

variable "vpc_cidr" {
  type    = string
  default = "10.20.0.0/16"
}

variable "az_count" {
  type    = number
  default = 2
}

variable "nat_gateway_count" {
  description = "1 for staging (cheaper), 2+ for production (one per AZ, no single point of failure)"
  type        = number
  default     = 1
}
