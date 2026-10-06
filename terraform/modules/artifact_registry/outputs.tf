output "repository_id" {
  value       = google_artifact_registry_repository.payment_repo.repository_id
  description = "The repository ID"
}

output "repository_url" {
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.payment_repo.repository_id}"
  description = "Full Docker image repository prefix"
}
