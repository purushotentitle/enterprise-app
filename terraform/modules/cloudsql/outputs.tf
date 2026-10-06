output "instance_name" {
  value       = google_sql_database_instance.postgres.name
  description = "The Cloud SQL instance name"
}

output "private_ip_address" {
  value       = google_sql_database_instance.postgres.private_ip_address
  description = "Private IP address for internal VPC database access"
}

output "database_name" {
  value       = google_sql_database.database.name
  description = "Application database name"
}

output "db_username" {
  value       = google_sql_user.user.name
  description = "Application database username"
}

output "secret_id" {
  value       = google_secret_manager_secret.db_secret.secret_id
  description = "Secret Manager secret ID for database credentials"
}
