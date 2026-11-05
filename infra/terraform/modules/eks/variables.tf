variable "name" { type = string }
variable "kubernetes_version" {
  type    = string
  default = "1.31"
}
variable "private_subnet_ids" { type = list(string) }
variable "public_subnet_ids" { type = list(string) }
variable "node_instance_types" {
  type    = list(string)
  default = ["t3.medium"]
}
variable "node_desired_size" {
  type    = number
  default = 2
}
variable "node_min_size" {
  type    = number
  default = 2
}
variable "node_max_size" {
  type    = number
  default = 6
}
variable "oidc_thumbprint" {
  description = "AWS's EKS OIDC root CA thumbprint (stable, documented by AWS; see infra/terraform/environments/*/README.md)"
  type        = string
  default     = "9e99a48a9960b14926bb7f3b02e22da2b0ab7280"
}
