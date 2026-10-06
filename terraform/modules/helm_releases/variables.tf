variable "project_id" {
  type        = string
  description = "GCP Project ID"
}

variable "environment" {
  type        = string
  description = "Deployment environment"
}

variable "app_name" {
  type        = string
  description = "Application name"
}

variable "helm_chart_version" {
  type        = string
  description = "Helm chart version"
}

variable "image_repository" {
  type        = string
  description = "Container image repository URL"
}

variable "image_tag" {
  type        = string
  description = "Container image tag"
  default     = "1.0.0"
}

variable "db_private_ip" {
  type        = string
  description = "Cloud SQL Private IP"
}

variable "db_name" {
  type        = string
  description = "Cloud SQL Database Name"
}

variable "db_username" {
  type        = string
  description = "Cloud SQL Database Username"
}

variable "db_secret_id" {
  type        = string
  description = "Secret Manager secret ID holding database credentials"
}

variable "cloud_armor_policy_name" {
  type        = string
  description = "Cloud Armor security policy name"
}

variable "domain_name" {
  type        = string
  description = "Domain name for ingress"
}
