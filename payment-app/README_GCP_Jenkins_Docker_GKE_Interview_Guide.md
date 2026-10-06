# GCP + Jenkins + Docker + Kubernetes
## Payment App — Interview-Ready Hands-On Guide

Goal: move the **same local Payment App flow** to Google Cloud without learning a completely new model.

---

## 1. One Mental Model

### Local

```text
Git
 ↓
Jenkins
 ↓
Docker Image
 ↓
Minikube
 ↓
Deployment
 ↓
Pod
 ↓
Service
 ↓
Payment API
```

### GCP

```text
Git
 ↓
Jenkins
 ↓
Docker Image
 ↓
Artifact Registry
 ↓
GKE
 ↓
Deployment
 ↓
Pod
 ↓
Service / Load Balancer
 ↓
Payment API
```

Monitoring:

```text
Payment App
   ↓ metrics/logs
Cloud Monitoring + Managed Prometheus
   ↓
Grafana / Cloud dashboards
```

> **Interview crux:** Jenkins builds. Docker packages. Artifact Registry stores. GKE runs. Service exposes. Monitoring observes.

---

## 2. Local → GCP Mapping

| Local Setup | GCP Equivalent |
|---|---|
| Docker Desktop | Docker still builds the image |
| Local image | Artifact Registry |
| Minikube | GKE |
| `minikube ip` | GKE Service external IP |
| Local Kubernetes context | `gcloud container clusters get-credentials` |
| Prometheus | Managed Service for Prometheus or your own Prometheus |
| Grafana | Existing Grafana can still use Prometheus-compatible metrics |
| Local logs | Cloud Logging |
| Kubernetes Secret | Secret Manager + Workload Identity for production |
| H2 in-memory DB | H2 for lab; Cloud SQL or another persistent DB for real use |

---

## 3. One-Time GCP Setup

Install:

```text
Google Cloud CLI
kubectl
gke-gcloud-auth-plugin
Docker
```

Login:

```bash
gcloud auth login
```

Set project:

```bash
gcloud config set project <PROJECT_ID>
```

Enable APIs:

```bash
gcloud services enable container.googleapis.com
gcloud services enable artifactregistry.googleapis.com
```

---

## 4. Create Artifact Registry

Artifact Registry stores your versioned Docker images.

```bash
gcloud artifacts repositories create payment-repo ^
  --repository-format=docker ^
  --location=asia-south1
```

Configure Docker authentication:

```bash
gcloud auth configure-docker asia-south1-docker.pkg.dev
```

Image format:

```text
REGION-docker.pkg.dev/PROJECT/REPOSITORY/IMAGE:TAG
```

Example:

```text
asia-south1-docker.pkg.dev/my-project/payment-repo/payment-service:1.0
```

> **Interview crux:** Artifact Registry is the image store between CI and GKE.

---

## 5. Build and Push the Payment Image

Build:

```bash
docker build -t payment-service:1.0 .
```

Tag:

```bash
docker tag payment-service:1.0 ^
asia-south1-docker.pkg.dev/<PROJECT_ID>/payment-repo/payment-service:1.0
```

Push:

```bash
docker push ^
asia-south1-docker.pkg.dev/<PROJECT_ID>/payment-repo/payment-service:1.0
```

Mental model:

```text
Source
 ↓
Docker Build
 ↓
Image
 ↓
Artifact Registry
```

---

## 6. Create a GKE Cluster

For learning, use a small Standard cluster so you can clearly see Node → Pod → Deployment.

```bash
gcloud container clusters create payment-cluster ^
  --zone=asia-south1-a ^
  --num-nodes=1
```

Connect `kubectl`:

```bash
gcloud container clusters get-credentials payment-cluster ^
  --zone=asia-south1-a
```

Verify:

```bash
kubectl get nodes
```

> **Interview crux:** GKE is managed Kubernetes. Your Kubernetes objects stay familiar; Google manages much of the cluster platform.

---

## 7. Change Only the Image in Your Deployment

Local:

```yaml
image: payment-service:latest
```

GCP:

```yaml
image: asia-south1-docker.pkg.dev/<PROJECT_ID>/payment-repo/payment-service:1.0
```

Deploy:

```bash
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
```

Verify:

```bash
kubectl get deployments
kubectl get pods
kubectl get svc
```

---

## 8. Expose the Payment App

In Minikube:

```text
minikube service payment-service --url
```

In GKE, use a Kubernetes Service with:

```yaml
type: LoadBalancer
```

Then:

```bash
kubectl get svc payment-service
```

Wait for:

```text
EXTERNAL-IP
```

Health URL:

```text
http://<EXTERNAL-IP>/actuator/health
```

Mental model:

```text
Internet
   ↓
Google Cloud Load Balancer
   ↓
Kubernetes Service
   ↓
Pods
```

---

## 9. Jenkins Still Fits

Your current Jenkins can remain the CI/CD controller.

```text
Git
 ↓
Jenkins
 ↓
mvn test/package
 ↓
docker build
 ↓
docker push → Artifact Registry
 ↓
kubectl deploy/update
 ↓
GKE
```

Pipeline stages:

```text
1. Checkout
2. Test
3. Build JAR
4. Build Docker image
5. Push to Artifact Registry
6. Deploy to GKE
7. Verify rollout
8. Health check
```

Deploy new image:

```bash
kubectl set image deployment/payment-service ^
payment-service=asia-south1-docker.pkg.dev/<PROJECT_ID>/payment-repo/payment-service:<TAG>
```

Verify:

```bash
kubectl rollout status deployment/payment-service
```

Rollback:

```bash
kubectl rollout undo deployment/payment-service
```

> **Interview crux:** Jenkins orchestrates the pipeline; GKE runs the application.

---

## 10. How Jenkins Authenticates to GCP

For manual lab use:

```bash
gcloud auth login
```

For CI/CD, do not depend on a developer login.

Production idea:

```text
Jenkins
   ↓
Workload / machine identity
   ↓
IAM
   ↓
Artifact Registry + GKE
```

Prefer short-lived identity or Workload Identity Federation where possible.

If Jenkins runs on Google Cloud, it can use a Compute Engine service account.

> **Interview crux:** Authentication tells GCP who Jenkins is. IAM decides what Jenkins can do.

---

## 11. IAM — Keep It Simple

Jenkins needs only the permissions required to:

```text
Push images → Artifact Registry
Deploy workloads → GKE
```

Do not give Jenkins:

```text
Project Owner
```

just to make deployment easier.

> **Interview crux:** Use least privilege.

---

## 12. Docker Permission in Jenkins

Your local workaround:

```bash
docker exec -u root elated_swirles chmod 666 /var/run/docker.sock
```

is fine for a lab, but not a good production pattern.

> **Interview answer:** Direct Docker socket access gives Jenkins powerful host-level access. In production I would isolate build agents and use controlled build credentials and permissions.

---

## 13. No More Minikube Certificate Copying

Local Minikube:

```text
Copy certs
Edit kubeconfig
```

GKE:

```text
Authenticate to Google Cloud
        ↓
Get cluster credentials
        ↓
kubectl talks to GKE
```

Command:

```bash
gcloud container clusters get-credentials payment-cluster ^
  --zone=asia-south1-a
```

Then:

```bash
kubectl get nodes
```

---

## 14. Prometheus + Grafana on GCP

Current local flow:

```text
Payment Service
 ↓
Prometheus
 ↓
Grafana
```

GCP option:

```text
Payment Service
 ↓
Managed Service for Prometheus
 ↓
Cloud Monitoring
 ↓
Grafana / Cloud dashboards
```

Your PromQL knowledge remains useful.

> **Interview crux:** Managed Prometheus reduces the operational work of running Prometheus storage at scale.

---

## 15. Logging

Still works:

```bash
kubectl logs <pod>
```

GKE can also send logs to:

```text
Cloud Logging
```

Mental model:

```text
kubectl logs = immediate Pod troubleshooting
Cloud Logging = centralized production logs
```

---

## 16. Secrets

Lab:

```text
Kubernetes Secret
```

Production GCP pattern:

```text
GKE Pod
   ↓
Workload Identity
   ↓
Secret Manager
```

> **Interview crux:** Give the workload an identity, then allow that identity to read only the secrets it needs.

---

## 17. Database

Current app:

```text
H2 in-memory database
```

Important:

```text
Pod dies
 ↓
H2 memory disappears
```

Real cloud design:

```text
Payment Service
     ↓
Cloud SQL / another persistent database
```

> **Interview answer:** Containers are disposable. Important business data belongs in persistent external storage.

---

## 18. Self-Healing in GKE

Same experiment as Minikube:

```bash
kubectl get pods
kubectl delete pod <payment-pod>
kubectl get pods -w
```

GKE recreates it because:

```text
Desired state = 1 Pod
Actual state = 0 Pods
```

> Kubernetes knowledge learned in Minikube transfers directly to GKE.

---

## 19. Scaling in GKE

Manual:

```bash
kubectl scale deployment payment-service --replicas=3
```

Production can also use:

```text
Horizontal Pod Autoscaler
```

Mental model:

```text
Load increases
 ↓
Metric crosses target
 ↓
More Pods
```

> **Architect warning:** More Pods help only if the database, Kafka, connection pools, and external APIs can handle the extra concurrency.

---

## 20. Troubleshooting — Same Flow

```text
Node
 ↓
Pod
 ↓
Events
 ↓
Logs
 ↓
Service
 ↓
Endpoints
 ↓
Application Health
```

Commands:

```bash
kubectl get nodes
kubectl get pods
kubectl describe pod <pod>
kubectl logs <pod>
kubectl logs <pod> --previous
kubectl get svc
kubectl get endpoints
```

GCP adds:

```text
Cloud Logging
Cloud Monitoring
GKE dashboards
```

---

## 21. Local Startup vs GCP

### Local

```text
Docker Desktop
 ↓
Minikube
 ↓
Jenkins
 ↓
Pods
 ↓
Service URL
```

### GCP

You normally do **not** recreate the cluster every morning.

```text
GKE already exists
 ↓
Authenticate
 ↓
Get cluster credentials
 ↓
Check Pods
 ↓
Deploy new image when required
```

Typical commands:

```bash
gcloud auth login

gcloud container clusters get-credentials payment-cluster ^
  --zone=asia-south1-a

kubectl get nodes
kubectl get pods
kubectl get svc
```

---

## 22. Full Interview Architecture

```text
Developer
   ↓
Git
   ↓
Jenkins
   ↓
Maven
   ↓
Docker Build
   ↓
Artifact Registry
   ↓
GKE Deployment
   ↓
ReplicaSet
   ↓
Pods
   ↓
Service
   ↓
Cloud Load Balancer
   ↓
Client
```

Async:

```text
Payment Service → Kafka → Consumers
```

Observability:

```text
GKE / App
   ↓
Cloud Logging + Managed Prometheus
   ↓
Cloud Monitoring / Grafana
```

Secrets:

```text
GKE Workload
   ↓
Workload Identity
   ↓
Secret Manager
```

---

## 23. Five GCP Interview Answers

### What replaces Minikube in GCP?

> GKE. Minikube is local Kubernetes for development. GKE is Google Cloud's managed Kubernetes service.

### Where do Docker images live?

> Jenkins builds the Docker image and pushes it to Artifact Registry. GKE pulls that versioned image when creating Pods.

### How does Jenkins deploy to GKE?

> Jenkins authenticates to Google Cloud, updates the Kubernetes Deployment to the new image, and verifies the rollout with kubectl.

### How does traffic reach the Payment Service?

> A cloud load balancer sends traffic to the Kubernetes Service, and the Service routes it to ready Payment Pods.

### How do you handle secrets?

> In production I prefer Workload Identity for the Pod and Secret Manager for sensitive values instead of putting cloud credentials inside images or source code.

---

## 24. One Architect-Level Answer

### Explain your GCP CI/CD architecture.

> A developer commits code to Git. Jenkins runs tests and builds the Spring Boot application. Jenkins creates an immutable Docker image and pushes it to Artifact Registry. Jenkins then updates the Kubernetes Deployment in GKE. GKE performs the rolling deployment and exposes ready Pods through a Service and cloud load balancer. Cloud Logging and Managed Service for Prometheus provide observability. IAM and workload identity control access to Google Cloud resources.

---

## 25. What to Practice

```text
1. Create Artifact Registry
2. Build Payment Docker image
3. Push image
4. Create GKE cluster
5. Connect kubectl
6. Deploy Payment Service
7. Expose LoadBalancer
8. Test /actuator/health
9. Delete Pod and watch recovery
10. Scale 1 → 3
11. Push image v2
12. Roll out v2
13. Roll back
14. View logs
15. Add Jenkins automation
16. Connect metrics
17. Move secrets to Secret Manager
```

---

## 26. Final Memory Map

```text
LOCAL
Docker → Minikube

GCP
Docker → Artifact Registry → GKE
```

Full:

```text
GIT
 ↓
JENKINS
 ↓
DOCKER
 ↓
ARTIFACT REGISTRY
 ↓
GKE
 ↓
DEPLOYMENT
 ↓
POD
 ↓
SERVICE
 ↓
LOAD BALANCER
 ↓
USER
```

Remember:

> **Jenkins builds, Docker packages, Artifact Registry stores, GKE runs, Service exposes, IAM secures, and Monitoring observes.**
