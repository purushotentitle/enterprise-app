output "vpc_network_name" {
  description = "The name of the VPC network"
  value       = module.vpc.network_name
}

output "gke_cluster_name" {
  description = "The name of the GKE cluster"
  value       = module.gke.cluster_name
}

output "gke_cluster_endpoint" {
  description = "The API endpoint of the GKE cluster"
  value       = module.gke.cluster_endpoint
}

output "cloudsql_instance_name" {
  description = "The Cloud SQL instance name"
  value       = module.cloudsql.instance_name
}

output "cloudsql_private_ip" {
  description = "The Cloud SQL private IP inside the VPC"
  value       = module.cloudsql.private_ip_address
}

output "artifact_registry_repo" {
  description = "Artifact Registry Docker repository URL"
  value       = module.artifact_registry.repository_url
}

output "cloud_armor_policy" {
  description = "Cloud Armor security policy name"
  value       = module.cloud_armor.security_policy_name
}

output "k8s_payment_namespace" {
  description = "Kubernetes namespace for the payment system"
  value       = module.helm_releases.namespace
}

output "workload_service_account" {
  description = "Workload Identity Service Account email"
  value       = module.helm_releases.workload_service_account
}

output "helm_release_status" {
  description = "Helm release name for payment app"
  value       = module.helm_releases.payment_app_release
}
