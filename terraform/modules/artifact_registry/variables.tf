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

variable "gke_service_account" {
  type        = string
  description = "GKE Node service account email"
}
