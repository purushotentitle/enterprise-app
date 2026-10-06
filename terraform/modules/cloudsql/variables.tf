variable "project_id" {
  type        = string
  description = "GCP Project ID"
}

variable "region" {
  type        = string
  description = "GCP Region"
}

variable "environment" {
  type        = string
  description = "Environment name"
}

variable "app_name" {
  type        = string
  description = "Application name"
}

variable "network_id" {
  type        = string
  description = "VPC network ID"
}

variable "tier" {
  type        = string
  description = "Machine tier for Cloud SQL"
  default     = "db-custom-4-16384"
}

variable "db_name" {
  type        = string
  description = "Database name"
  default     = "paymentdb"
}

variable "db_username" {
  type        = string
  description = "Database username"
  default     = "payment_user"
}
