variable "project_id" {
  description = "The GCP Project ID where resources will be provisioned"
  type        = string
}

variable "region" {
  description = "The GCP Region for the Regional GKE cluster and Cloud SQL (HA across 3 zones)"
  type        = string
  default     = "us-central1"
}

variable "environment" {
  description = "Deployment environment name (e.g., prod, staging)"
  type        = string
  default     = "prod"
}

variable "app_name" {
  description = "Application name used for naming conventions and tagging"
  type        = string
  default     = "payment-app"
}

variable "gke_node_pool_min_count" {
  description = "Minimum number of nodes per zone in the application node pool"
  type        = number
  default     = 2
}

variable "gke_node_pool_max_count" {
  description = "Maximum number of nodes per zone in the application node pool"
  type        = number
  default     = 6
}

variable "gke_machine_type" {
  description = "Machine type for the GKE worker nodes"
  type        = string
  default     = "e2-standard-4"
}

variable "db_tier" {
  description = "Cloud SQL machine tier for the production PostgreSQL instance"
  type        = string
  default     = "db-custom-4-16384"
}

variable "db_name" {
  description = "PostgreSQL Database name for the Payment Service"
  type        = string
  default     = "paymentdb"
}

variable "db_username" {
  description = "PostgreSQL Database user"
  type        = string
  default     = "payment_user"
}

variable "helm_chart_version" {
  description = "Version of the payment-app Helm chart"
  type        = string
  default     = "1.0.0"
}

variable "domain_name" {
  description = "Production domain name for ingress routing and TLS"
  type        = string
  default     = "payment.example.com"
}
