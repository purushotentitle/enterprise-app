output "cluster_name" {
  value       = google_container_cluster.primary.name
  description = "The GKE cluster name"
}

output "cluster_id" {
  value       = google_container_cluster.primary.id
  description = "The GKE cluster ID"
}

output "cluster_endpoint" {
  value       = google_container_cluster.primary.endpoint
  description = "The GKE cluster API endpoint"
}

output "cluster_ca_certificate" {
  value       = google_container_cluster.primary.master_auth[0].cluster_ca_certificate
  description = "The GKE cluster CA certificate (base64 encoded)"
  sensitive   = true
}

output "workload_identity_pool" {
  value       = "${var.project_id}.svc.id.goog"
  description = "The Workload Identity pool"
}

output "node_service_account" {
  value       = google_service_account.gke_nodes_sa.email
  description = "The service account email used by GKE nodes"
}
