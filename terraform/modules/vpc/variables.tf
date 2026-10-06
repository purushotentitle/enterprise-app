variable "environment" {
  type        = string
  description = "Environment name"
}

variable "app_name" {
  type        = string
  description = "Application name"
}

variable "region" {
  type        = string
  description = "GCP Region"
}

variable "subnet_cidr" {
  type        = string
  description = "CIDR range for node primary subnet"
  default     = "10.10.0.0/20"
}

variable "pods_cidr" {
  type        = string
  description = "Secondary CIDR range for GKE Pods"
  default     = "10.20.0.0/14"
}

variable "services_cidr" {
  type        = string
  description = "Secondary CIDR range for GKE Services"
  default     = "10.30.0.0/20"
}
