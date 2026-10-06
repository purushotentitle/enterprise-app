output "network_id" {
  value       = google_compute_network.vpc.id
  description = "The ID of the VPC"
}

output "network_name" {
  value       = google_compute_network.vpc.name
  description = "The name of the VPC"
}

output "subnet_id" {
  value       = google_compute_subnetwork.gke_subnet.id
  description = "The ID of the GKE subnetwork"
}

output "subnet_name" {
  value       = google_compute_subnetwork.gke_subnet.name
  description = "The name of the GKE subnetwork"
}

output "pods_range_name" {
  value       = "${var.environment}-gke-pods"
  description = "The secondary range name for pods"
}

output "services_range_name" {
  value       = "${var.environment}-gke-services"
  description = "The secondary range name for services"
}
