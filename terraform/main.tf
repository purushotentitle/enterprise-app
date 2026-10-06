# ── MODULE 1: VPC Network & Cloud NAT (Private Networking) ─────────────
module "vpc" {
  source      = "./modules/vpc"
  project_id  = var.project_id
  region      = var.region
  environment = var.environment
  app_name    = var.app_name
}

# ── MODULE 2: Regional GKE Cluster (Multi-AZ Control Plane & Nodes) ───
module "gke" {
  source              = "./modules/gke"
  project_id          = var.project_id
  region              = var.region
  environment         = var.environment
  app_name            = var.app_name
  network_id          = module.vpc.network_id
  subnet_id           = module.vpc.subnet_id
  pods_range_name     = module.vpc.pods_range_name
  services_range_name = module.vpc.services_range_name
  node_pool_min_count = var.gke_node_pool_min_count
  node_pool_max_count = var.gke_node_pool_max_count
  machine_type        = var.gke_machine_type

  depends_on = [module.vpc]
}

# ── MODULE 3: Artifact Registry (Secure Container Repository) ─────────
module "artifact_registry" {
  source              = "./modules/artifact_registry"
  project_id          = var.project_id
  region              = var.region
  environment         = var.environment
  app_name            = var.app_name
  gke_service_account = module.gke.node_service_account

  depends_on = [module.gke]
}

# ── MODULE 4: Cloud SQL PostgreSQL (HA Multi-AZ Automatic Failover) ────
module "cloudsql" {
  source      = "./modules/cloudsql"
  project_id  = var.project_id
  region      = var.region
  environment = var.environment
  app_name    = var.app_name
  network_id  = module.vpc.network_id
  tier        = var.db_tier
  db_name     = var.db_name
  db_username = var.db_username

  depends_on = [module.vpc]
}

# ── MODULE 5: Cloud Armor WAF & Anti-DDoS Security Policy ─────────────
module "cloud_armor" {
  source      = "./modules/cloud_armor"
  environment = var.environment
  app_name    = var.app_name
}

# ── MODULE 6: Helm Releases & Workload Identity Kubernetes Integration ─
module "helm_releases" {
  source                  = "./modules/helm_releases"
  project_id              = var.project_id
  environment             = var.environment
  app_name                = var.app_name
  helm_chart_version      = var.helm_chart_version
  image_repository        = "${module.artifact_registry.repository_url}/${var.app_name}"
  image_tag               = "1.0.0"
  db_private_ip           = module.cloudsql.private_ip_address
  db_name                 = module.cloudsql.database_name
  db_username             = module.cloudsql.db_username
  db_secret_id            = module.cloudsql.secret_id
  cloud_armor_policy_name = module.cloud_armor.security_policy_name
  domain_name             = var.domain_name

  depends_on = [
    module.gke,
    module.cloudsql,
    module.artifact_registry,
    module.cloud_armor
  ]
}
