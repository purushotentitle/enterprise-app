resource "google_compute_network" "vpc" {
  name                    = "${var.environment}-${var.app_name}-vpc"
  auto_create_subnetworks = false
  routing_mode            = "REGIONAL"
  description             = "Dedicated VPC for ${var.app_name} High-Availability production deployment"
}

resource "google_compute_subnetwork" "gke_subnet" {
  name                     = "${var.environment}-${var.app_name}-subnet"
  ip_cidr_range            = var.subnet_cidr
  region                   = var.region
  network                  = google_compute_network.vpc.id
  private_ip_google_access = true

  secondary_ip_range {
    range_name    = "${var.environment}-gke-pods"
    ip_cidr_range = var.pods_cidr
  }

  secondary_ip_range {
    range_name    = "${var.environment}-gke-services"
    ip_cidr_range = var.services_cidr
  }

  log_config {
    aggregation_interval = "INTERVAL_5_SEC"
    flow_sampling        = 0.5
    metadata             = "INCLUDE_ALL_METADATA"
  }
}

# Cloud Router for High Availability Outbound NAT
resource "google_compute_router" "router" {
  name    = "${var.environment}-${var.app_name}-router"
  region  = var.region
  network = google_compute_network.vpc.id
}

# Cloud NAT for private GKE nodes to reach internet securely without public IP
resource "google_compute_router_nat" "nat" {
  name                               = "${var.environment}-${var.app_name}-nat"
  router                             = google_compute_router.router.name
  region                             = var.region
  nat_ip_allocate_option             = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat = "ALL_SUBNETWORKS_ALL_IP_RANGES"

  log_config {
    enable = true
    filter = "ERRORS_ONLY"
  }
}

# Firewall rule: Allow health checks from GCP Load Balancer to Ingress/GKE
resource "google_compute_firewall" "allow_gcp_health_checks" {
  name    = "${var.environment}-${var.app_name}-allow-gcp-health-checks"
  network = google_compute_network.vpc.name

  allow {
    protocol = "tcp"
    ports    = ["80", "443", "8080", "10254"]
  }

  source_ranges = [
    "35.191.0.0/16",
    "130.211.0.0/22"
  ]

  description = "Allow GCP Health Checks from Google Load Balancer ranges"
}

# Firewall rule: Intra-cluster pod communication
resource "google_compute_firewall" "allow_internal" {
  name    = "${var.environment}-${var.app_name}-allow-internal"
  network = google_compute_network.vpc.name

  allow {
    protocol = "icmp"
  }
  allow {
    protocol = "tcp"
  }
  allow {
    protocol = "udp"
  }

  source_ranges = [
    var.subnet_cidr,
    var.pods_cidr,
    var.services_cidr
  ]

  description = "Allow internal traffic within VPC and GKE secondary ranges"
}
