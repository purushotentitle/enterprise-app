output "namespace" {
  value       = kubernetes_namespace.payment_system.metadata[0].name
  description = "Kubernetes namespace for the payment application"
}

output "payment_app_release" {
  value       = helm_release.payment_app.name
  description = "Helm release name for payment app"
}

output "kafka_release" {
  value       = helm_release.kafka_ha.name
  description = "Helm release name for Kafka HA cluster"
}

output "workload_service_account" {
  value       = google_service_account.payment_workload_sa.email
  description = "GCP Workload Identity Service Account email"
}
