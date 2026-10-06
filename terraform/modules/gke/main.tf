# Dedicated Google Service Account for GKE Nodes
resource "google_service_account" "gke_nodes_sa" {
  account_id   = "${var.environment}-${var.app_name}-gke-sa"
  display_name = "GKE Worker Nodes SA for ${var.app_name}"
}

# Grant minimal permissions to nodes
resource "google_project_iam_member" "node_sa_roles" {
  for_each = toset([
    "roles/logging.logWriter",
    "roles/monitoring.metricWriter",
    "roles/monitoring.viewer",
    "roles/stackdriver.resourceMetadata.writer",
    "roles/artifactregistry.reader"
  ])
  project = var.project_id
  role    = each.key
  member  = "serviceAccount:${google_service_account.gke_nodes_sa.email}"
}

# Regional High-Availability GKE Cluster (Control plane replicated across 3 zones)
resource "google_container_cluster" "primary" {
  name     = "${var.environment}-${var.app_name}-cluster"
  location = var.region # Regional cluster for HA control plane across 3 AZs

  # Remove default node pool immediately to manage dedicated pools with granular specs
  remove_default_node_pool = true
  initial_node_count       = 1

  network    = var.network_id
  subnetwork = var.subnet_id

  # IP Allocation Policy for VPC-native traffic routing
  ip_allocation_policy {
    cluster_secondary_range_name  = var.pods_range_name
    services_secondary_range_name = var.services_range_name
  }

  # Dataplane V2 (Cilium) for high-performance eBPF networking & native NetworkPolicy
  datapath_provider = "ADVANCED_DATAPATH"

  # Workload Identity for keyless, secure authentication from Pods to GCP APIs
  workload_identity_config {
    workload_pool = "${var.project_id}.svc.id.goog"
  }

  # Private Cluster Configuration
  private_cluster_config {
    enable_private_nodes    = true
    enable_private_endpoint = false # Set to true with Cloud Interconnect / VPN if internal-only
    master_ipv4_cidr_block  = "172.16.0.0/28"
  }

  # Release channel for automatic security patch management
  release_channel {
    channel = "REGULAR"
  }

  # Production Add-ons
  addons_config {
    http_load_balancing {
      disabled = false
    }
    horizontal_pod_autoscaling {
      disabled = false
    }
    network_policy_config {
      disabled = false
    }
    gce_persistent_disk_csi_driver_config {
      enabled = true
    }
  }

  # Logging and Monitoring
  logging_config {
    enable_components = ["SYSTEM_COMPONENTS", "WORKLOADS"]
  }

  monitoring_config {
    enable_components = ["SYSTEM_COMPONENTS"]
    managed_prometheus {
      enabled = true # Google Cloud Managed Service for Prometheus
    }
  }

  # Master Authorized Networks for control plane access hardening
  master_authorized_networks_config {
    cidr_blocks {
      cidr_block   = "0.0.0.0/0" # In production, restrict to CI/CD and Corporate VPN CIDRs
      display_name = "Authorized Management Access"
    }
  }

  maintenance_policy {
    recurring_window {
      start_time = "2026-01-01T04:00:00Z"
      end_time   = "2026-01-01T08:00:00Z"
      recurrence = "FREQ=WEEKLY;BYDAY=SA,SU"
    }
  }
}

# High-Availability Application Node Pool (spread across all 3 zones)
resource "google_container_node_pool" "app_pool" {
  name       = "${var.environment}-${var.app_name}-app-pool"
  location   = var.region
  cluster    = google_container_cluster.primary.name

  autoscaling {
    min_node_count = var.node_pool_min_count
    max_node_count = var.node_pool_max_count
  }

  management {
    auto_repair  = true
    auto_upgrade = true
  }

  upgrade_settings {
    max_surge       = 1
    max_unavailable = 0
    strategy        = "SURGE"
  }

  node_config {
    machine_type = var.machine_type
    disk_size_gb = 100
    disk_type    = "pd-balanced"
    image_type   = "COS_CONTAINERD"

    service_account = google_service_account.gke_nodes_sa.email
    oauth_scopes    = ["https://www.googleapis.com/auth/cloud-platform"]

    # Production Shielded VM Settings
    shielded_instance_config {
      enable_secure_boot          = true
      enable_integrity_monitoring = true
    }

    labels = {
      environment = var.environment
      role        = "payment-worker"
      app         = var.app_name
    }

    metadata = {
      disable-legacy-endpoints = "true"
    }

    tags = ["gke-node", "${var.environment}-${var.app_name}"]
  }
}
