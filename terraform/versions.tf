terraform {
  required_version = ">= 1.5.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.25.0"
    }
    google-beta = {
      source  = "hashicorp/google-beta"
      version = "~> 5.25.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.29.0"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.13.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6.0"
    }
  }

  # For production, configure remote state in Cloud Storage (GCS)
  # backend "gcs" {
  #   bucket = "tf-state-payment-app-prod"
  #   prefix = "terraform/state"
  # }
}
