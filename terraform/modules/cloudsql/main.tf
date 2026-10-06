# Reserve private IP range for VPC peering to Google services (Cloud SQL)
resource "google_compute_global_address" "private_ip_address" {
  name          = "${var.environment}-${var.app_name}-db-private-ip"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = var.network_id
}

# Establish private VPC peering with Google Service Networking
resource "google_service_networking_connection" "private_vpc_connection" {
  network                 = var.network_id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_ip_address.name]
}

# Generate secure random password for PostgreSQL payment user
resource "random_password" "db_password" {
  length  = 24
  special = false
}

# Store database credentials in GCP Secret Manager for Workload Identity retrieval
resource "google_secret_manager_secret" "db_secret" {
  secret_id = "${var.environment}-${var.app_name}-db-credentials"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "db_secret_version" {
  secret = google_secret_manager_secret.db_secret.id
  secret_data = jsonencode({
    username = var.db_username
    password = random_password.db_password.result
    database = var.db_name
  })
}

# Regional High Availability Cloud SQL PostgreSQL Instance
resource "google_sql_database_instance" "postgres" {
  name             = "${var.environment}-${var.app_name}-pg-${substr(uuid(), 0, 8)}"
  database_version = "POSTGRES_15"
  region           = var.region

  depends_on = [google_service_networking_connection.private_vpc_connection]

  settings {
    tier              = var.tier
    availability_type = "REGIONAL" # High Availability with automatic multi-zone failover
    disk_type         = "PD_SSD"
    disk_size         = 50
    disk_autoresize   = true

    ip_configuration {
      ipv4_enabled                                  = false # Zero public IP exposure
      private_network                               = var.network_id
      enable_private_path_for_google_cloud_services = true
    }

    backup_configuration {
      enabled                        = true
      start_time                     = "03:00"
      point_in_time_recovery_enabled = true
      transaction_log_retention_days = 7
      backup_retention_settings {
        retained_backups = 30
      }
    }

    insights_config {
      query_insights_enabled  = true
      query_string_length     = 1024
      record_application_tags = true
      record_client_address   = true
    }

    maintenance_window {
      day          = 7 # Sunday
      hour         = 3
      update_track = "stable"
    }

    database_flags {
      name  = "max_connections"
      value = "200"
    }

    database_flags {
      name  = "log_connections"
      value = "on"
    }

    database_flags {
      name  = "log_disconnections"
      value = "on"
    }
  }

  deletion_protection = false # Set true in production account lock
}

# Application Database
resource "google_sql_database" "database" {
  name     = var.db_name
  instance = google_sql_database_instance.postgres.name
}

# Application Database User
resource "google_sql_user" "user" {
  name     = var.db_username
  instance = google_sql_database_instance.postgres.name
  password = random_password.db_password.result
}
