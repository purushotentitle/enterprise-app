# ── Kubernetes Namespaces ──────────────────────────────────────────────
resource "kubernetes_namespace" "payment_system" {
  metadata {
    name = "payment-system"
    labels = {
      name                                    = "payment-system"
      "pod-security.kubernetes.io/enforce"    = "baseline"
      "pod-security.kubernetes.io/audit"      = "restricted"
      "pod-security.kubernetes.io/warn"       = "restricted"
    }
  }
}

resource "kubernetes_namespace" "monitoring" {
  metadata {
    name = "monitoring"
  }
}

# ── GCP Service Account & Workload Identity Integration ───────────────
resource "google_service_account" "payment_workload_sa" {
  account_id   = "${var.environment}-${var.app_name}-app-sa"
  display_name = "Workload Identity SA for ${var.app_name}"
}

# Allow K8s ServiceAccount to impersonate the GCP ServiceAccount
resource "google_service_account_iam_member" "workload_identity_user" {
  service_account_id = google_service_account.payment_workload_sa.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.project_id}.svc.id.goog[${kubernetes_namespace.payment_system.metadata[0].name}/${var.app_name}-sa]"
}

# Grant Cloud SQL Client permission
resource "google_project_iam_member" "cloudsql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.payment_workload_sa.email}"
}

# Grant Secret Manager Accessor for database secrets
resource "google_secret_manager_secret_iam_member" "secret_accessor" {
  project   = var.project_id
  secret_id = var.db_secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.payment_workload_sa.email}"
}

# ── HA Kafka Helm Release (3-node HA cluster across AZs) ─────────────
resource "helm_release" "kafka_ha" {
  name       = "kafka-cluster"
  repository = "https://charts.bitnami.com/bitnami"
  chart      = "kafka"
  version    = "28.0.0"
  namespace  = kubernetes_namespace.payment_system.metadata[0].name
  timeout    = 600

  set {
    name  = "listeners.client.protocol"
    value = "PLAINTEXT"
  }
  set {
    name  = "controller.replicaCount"
    value = "3" # High Availability quorum across 3 zones
  }
  set {
    name  = "broker.replicaCount"
    value = "3" # 3 brokers across 3 availability zones
  }
  set {
    name  = "broker.persistence.size"
    value = "50Gi"
  }
  set {
    name  = "broker.persistence.storageClass"
    value = "standard-rwo"
  }
  set {
    name  = "broker.podAntiAffinityPreset"
    value = "hard" # Strictly separate zones
  }
  set {
    name  = "defaultReplicationFactor"
    value = "3"
  }
  set {
    name  = "offsetsTopicReplicationFactor"
    value = "3"
  }
  set {
    name  = "transactionStateLogReplicationFactor"
    value = "3"
  }
  set {
    name  = "transactionStateLogMinIsr"
    value = "2"
  }
}

# ── Ingress-NGINX High Availability Release ───────────────────────────
resource "helm_release" "ingress_nginx" {
  name       = "ingress-nginx"
  repository = "https://kubernetes.github.io/ingress-nginx"
  chart      = "ingress-nginx"
  version    = "4.10.0"
  namespace  = "ingress-nginx"
  create_namespace = true

  set {
    name  = "controller.replicaCount"
    value = "3" # Multi-zone HA controller pods
  }
  set {
    name  = "controller.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].weight"
    value = "100"
  }
  set {
    name  = "controller.metrics.enabled"
    value = "true"
  }
}

# ── Payment App Helm Release ──────────────────────────────────────────
resource "helm_release" "payment_app" {
  name       = var.app_name
  chart      = "${path.module}/../../../helm/payment-app"
  namespace  = kubernetes_namespace.payment_system.metadata[0].name
  version    = var.helm_chart_version
  timeout    = 600

  depends_on = [
    helm_release.kafka_ha,
    helm_release.ingress_nginx,
    google_service_account_iam_member.workload_identity_user
  ]

  values = [
    yamlencode({
      replicaCount = 3
      image = {
        repository = var.image_repository
        tag        = var.image_tag
        pullPolicy = "IfNotPresent"
      }
      serviceAccount = {
        create = true
        name   = "${var.app_name}-sa"
        annotations = {
          "iam.gke.io/gcp-service-account" = google_service_account.payment_workload_sa.email
        }
      }
      env = {
        SPRING_PROFILES_ACTIVE         = "prod"
        SPRING_KAFKA_BOOTSTRAP_SERVERS = "kafka-cluster:9092"
        SPRING_DATASOURCE_URL          = "jdbc:postgresql://${var.db_private_ip}:5432/${var.db_name}"
        SPRING_DATASOURCE_USERNAME     = var.db_username
      }
      cloudArmor = {
        enabled    = true
        policyName = var.cloud_armor_policy_name
      }
      ingress = {
        enabled   = true
        className = "nginx"
        hosts = [
          {
            host = var.domain_name
            paths = [
              {
                path     = "/"
                pathType = "Prefix"
              }
            ]
          }
        ]
        tls = [
          {
            secretName = "${var.app_name}-tls-cert"
            hosts      = [var.domain_name]
          }
        ]
      }
      autoscaling = {
        enabled                        = true
        minReplicas                    = 3
        maxReplicas                    = 12
        targetCPUUtilizationPercentage = 70
        targetMemoryUtilizationPercentage = 75
      }
      podDisruptionBudget = {
        enabled      = true
        minAvailable = 2
      }
      podAntiAffinity = {
        enabled = true
        topologyKey = "topology.kubernetes.io/zone"
      }
    })
  ]
}
