variable "name" { type = string }
variable "vpc_id" { type = string }
variable "private_subnet_ids" { type = list(string) }
variable "eks_node_security_group_id" { type = string }

variable "db_name" {
  type    = string
  default = "reliefgrid"
}
variable "db_username" {
  type    = string
  default = "reliefgrid"
}
variable "instance_class" {
  type    = string
  default = "db.t4g.micro"
}
variable "allocated_storage_gb" {
  type    = number
  default = 20
}
variable "max_allocated_storage_gb" {
  type    = number
  default = 100
}
variable "multi_az" {
  type    = bool
  default = false
}
variable "deletion_protection" {
  type    = bool
  default = false
}
variable "backup_retention_days" {
  type    = number
  default = 7
}
