resource "google_artifact_registry_repository" "payment_repo" {
  location      = var.region
  repository_id = "${var.environment}-${var.app_name}-repo"
  description   = "Docker registry for ${var.app_name} microservice images"
  format        = "DOCKER"

  docker_config {
    immutable_tags = false
  }
}

resource "google_artifact_registry_repository_iam_member" "gke_pull" {
  project    = var.project_id
  location   = google_artifact_registry_repository.payment_repo.location
  repository = google_artifact_registry_repository.payment_repo.name
  role       = "roles/artifactregistry.reader"
  member     = "serviceAccount:${var.gke_service_account}"
}
