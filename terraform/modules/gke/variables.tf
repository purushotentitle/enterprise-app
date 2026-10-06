variable "project_id" {
  type        = string
  description = "GCP Project ID"
}

variable "region" {
  type        = string
  description = "GCP Region for the Regional cluster"
}

variable "environment" {
  type        = string
  description = "Deployment environment"
}

variable "app_name" {
  type        = string
  description = "Application name"
}

variable "network_id" {
  type        = string
  description = "VPC network ID"
}

variable "subnet_id" {
  type        = string
  description = "Subnet ID for GKE"
}

variable "pods_range_name" {
  type        = string
  description = "Secondary range name for Pods"
}

variable "services_range_name" {
  type        = string
  description = "Secondary range name for Services"
}

variable "node_pool_min_count" {
  type        = number
  description = "Min nodes per zone"
  default     = 2
}

variable "node_pool_max_count" {
  type        = number
  description = "Max nodes per zone"
  default     = 6
}

variable "machine_type" {
  type        = string
  description = "Compute instance machine type"
  default     = "e2-standard-4"
}
