variable "role_name" { type = string }
variable "oidc_provider_arn" { type = string }
variable "oidc_provider_url" {
  description = "The issuer URL without the https:// prefix, e.g. oidc.eks.us-east-1.amazonaws.com/id/XXXX"
  type        = string
}
variable "namespace" { type = string }
variable "service_account_name" { type = string }
variable "policy_json" { type = string }
