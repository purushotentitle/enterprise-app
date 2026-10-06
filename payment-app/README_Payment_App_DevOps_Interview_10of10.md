# Payment App DevOps Guide
## Docker + Jenkins + Kubernetes + GCP — One Mental Model for Hands-On Learning and Interviews

> **Goal:** Understand one end-to-end system. Do not memorize separate Docker, Jenkins, Kubernetes, Kafka, Prometheus, Grafana, and GCP notes.

---


## Interview Scope

This guide is intentionally optimized for a **Java Senior Developer / Technical Architect / Solution Architect** interview.

It covers the Docker, Kubernetes, Jenkins, and GCP knowledge needed to explain, deploy, troubleshoot, scale, secure, and operate this Payment App. It intentionally avoids deep cluster-administrator topics that do not improve this interview story.

---

# 1. The ONE Mental Model

Everything in this guide belongs to this picture.

```text
DEVELOPER
   │
   ▼
GIT / SOURCE CODE
   │
   ▼
JENKINS  ← CI/CD controller
   │
   ├── Maven test + package
   │        │
   │        ▼
   │   Spring Boot JAR
   │
   └── Docker build
            │
            ▼
       DOCKER IMAGE
            │
            ▼
   ┌──────────────────────────────┐
   │ WHERE IS THE IMAGE STORED?   │
   │ Local lab: local Docker      │
   │ GCP: Artifact Registry       │
   └──────────────────────────────┘
            │
            ▼
       KUBERNETES
   Local = Minikube
   GCP   = GKE
            │
            ▼
       DEPLOYMENT
            │
            ▼
       REPLICASET
            │
            ▼
          POD
            │
            ▼
       CONTAINER
   runs Payment Service
            │
            ▼
         SERVICE
            │
            ▼
   Local: Minikube URL
   GCP: Load Balancer IP
            │
            ▼
          USER
```

The running Payment Service also uses:

```text
Payment Service ──events──> Kafka ──> Consumers

Payment Service ──data────> H2          [local lab]
Payment Service ──data────> Cloud SQL   [typical persistent GCP option]

Payment Service ──metrics─> Prometheus ──> Grafana
                                      or
                              Managed Prometheus
                                      ↓
                              Cloud Monitoring

Pod / App ──logs──────────> kubectl logs
                                      or
                                Cloud Logging

Configuration ────────────> ConfigMap
Sensitive configuration ─> Secret
GCP production secrets ──> Secret Manager + workload identity
```

## The complete interview sentence

> **Git stores the code. Jenkins builds and deploys it. Maven creates the JAR. Docker packages it as an image. A registry stores the image. Kubernetes runs that image in Pods through a Deployment. A Service exposes the Pods. Kafka handles asynchronous events, and Prometheus/Grafana provide observability. In GCP, Artifact Registry stores the image and GKE runs Kubernetes.**

---

# 2. Build Time vs Run Time

## Build / Deployment Path

```text
Git → Jenkins → Maven → JAR → Docker Build → Docker Image → Registry → Kubernetes Deployment
```

## Runtime Path

```text
User → Service / Load Balancer → Pod → Container → Spring Boot Payment Service → Database / Kafka
```

> **Memory:** Jenkins belongs mainly to the build/deploy path, not inside the user request path.

---

# 3. What Each Component Does

- **Git** — stores source code and version history.
- **Jenkins** — CI/CD orchestrator: checkout, test, build, image, push, deploy, verify.
- **Maven** — builds and tests the Java application.
- **Dockerfile** — instructions for creating the image.
- **Docker Image** — packaged application.
- **Container** — running instance of the image.
- **Image Registry** — stores versioned images.
- **Kubernetes Cluster** — runs and manages workloads.
- **Deployment** — declares desired version and replica count.
- **ReplicaSet** — keeps the required number of Pods running.
- **Pod** — smallest Kubernetes deployable unit.
- **Service** — stable network access to changing Pods.
- **Kafka** — asynchronous event communication.
- **Prometheus** — collects metrics.
- **Grafana** — visualizes metrics.
- **ConfigMap** — normal external configuration.
- **Secret** — sensitive Kubernetes configuration.

Easy memory:

```text
Java Class ≈ Docker Image
Java Object ≈ Docker Container
```

---

# 4. Why Kubernetes Exists

```text
Desired Pods = 1
Actual Pods  = 0
       ↓
Kubernetes creates another Pod
```

> **Interview crux:** Kubernetes continuously tries to make actual state match desired state.

---

# 5. Deployment → ReplicaSet → Pod → Container

```text
Deployment
   ↓
ReplicaSet
   ↓
Pod
   ↓
Container
   ↓
Spring Boot Process
```

### Interview answer

> A Deployment defines the desired version and replica count. It manages a ReplicaSet, which maintains the required Pods. Each Pod runs the application container.

---

# 6. Your Local Environment

```text
Windows
   ↓
Docker Desktop
   ├── Jenkins Container
   └── Minikube Kubernetes
           ↓
       Payment Pods
           ↓
       Payment Service
```

Supporting tools:

```text
Kafka UI   → localhost:8090
Jenkins    → localhost:8888
Prometheus → Minikube Service
Grafana    → Minikube Service
```

---

# 7. Local Startup — Clean Sequence

## 1. Start Docker Desktop

Docker must run first because Minikube uses the Docker driver and Jenkins is also a Docker container.

## 2. Start Minikube

```bash
minikube start --driver=docker
kubectl get nodes
```

Expected:

```text
minikube   Ready
```

## 3. Start Jenkins

```bash
docker start elated_swirles
```

Open:

```text
http://localhost:8888
```

## 4. Apply Your Lab-Specific Jenkins Access

```bash
docker exec -u root elated_swirles chmod 666 /var/run/docker.sock
```

Your Jenkins container also needs Minikube Kubernetes credentials.

Verify:

```bash
docker exec elated_swirles kubectl get nodes
```

> **Interview note:** Docker socket `chmod 666` is a lab shortcut, not a production security design.

## 5. Verify Kubernetes

```bash
kubectl get pods
kubectl get svc
```

## 6. Get Local URLs

```bash
minikube service payment-service --url
minikube service prometheus --url
minikube service grafana --url
```

## 7. Verify the App

```text
<PAYMENT_URL>/actuator/health
```

Expected:

```json
{"status":"UP"}
```

## 8. Verify Monitoring

```text
Prometheus → Status → Targets → payment-service = UP
Grafana    → Payment Service Dashboard
```

---

# 8. Troubleshoot Before Restarting

Do not make this your first response:

```bash
kubectl delete pods --all
```

Use:

```text
GET → DESCRIBE → LOGS → SERVICE → HEALTH
```

Commands:

```bash
kubectl get pods
kubectl describe pod <pod>
kubectl logs <pod>
kubectl logs <pod> --previous
kubectl get svc
kubectl get endpoints
```

Then test:

```text
/actuator/health
```

---

# 9. Docker — Remember 5 Questions

```text
What images exist?
What containers are running?
Why did one fail?
How do I enter it?
How do I start or stop it?
```

```bash
docker images
docker ps
docker logs <container>
docker exec -it <container> bash
docker start <container>
docker stop <container>
```

---

# 10. Kubernetes — Remember 7 Questions

```text
What is running?
Where is it running?
Why is it failing?
How does traffic reach it?
What configuration is it using?
Can Kubernetes recover it?
Can Kubernetes scale it?
```

```bash
kubectl get pods
kubectl get pods -o wide
kubectl describe pod <pod>
kubectl logs <pod>
kubectl get svc
kubectl get configmap
kubectl scale deployment payment-service --replicas=3
```

---

# 11. Self-Healing

```bash
kubectl get pods
kubectl delete pod <payment-pod>
kubectl get pods -w
```

> **Interview answer:** The Deployment declares the desired replica count. When a Pod disappears, Kubernetes detects the mismatch and creates a replacement.

---

# 12. Scaling

```bash
kubectl scale deployment payment-service --replicas=3
kubectl get pods
```

Return:

```bash
kubectl scale deployment payment-service --replicas=1
```

> **Architect point:** More Pods do not automatically mean more throughput. The real bottleneck may be the database, Kafka, a connection pool, or an external API.

---

# 13. Service, Labels, and Endpoints

```text
Service selector
      ↓
Pod label
```

Check:

```bash
kubectl get pods --show-labels
kubectl describe service payment-service
kubectl get endpoints
```

If labels do not match:

```text
Service → No endpoints → No application traffic
```

---

# 14. ConfigMap and Secret

```text
Image     = code
ConfigMap = normal configuration
Secret    = sensitive configuration
```

> **Interview crux:** Build one immutable image and inject environment-specific configuration at runtime.

---

# 15. Liveness vs Readiness

```text
Liveness  = Is the container alive?
Readiness = Can this Pod receive traffic?
```

> **Memory line:** Liveness decides restart. Readiness decides traffic.

---

# 16. Common Kubernetes Failures

## CrashLoopBackOff

```text
Container starts → Application crashes → Kubernetes restarts it
```

Check:

```bash
kubectl logs <pod> --previous
kubectl describe pod <pod>
```

## ImagePullBackOff

> Kubernetes cannot get the image.

Typical causes:

```text
Wrong image/tag
Registry authentication issue
Image missing
Registry unavailable
```

## Pending

> Kubernetes cannot schedule the Pod.

Typical causes:

```text
Insufficient CPU
Insufficient memory
Volume problem
Scheduling rule
```

## Running Pod but API Fails

Check:

```text
Pod → Application → Service → Endpoints → Network
```

---

# 17. Rolling Deployment

```bash
kubectl rollout status deployment/payment-service
kubectl rollout history deployment/payment-service
kubectl rollout undo deployment/payment-service
```

> **Interview answer:** A Deployment gradually replaces old Pods with the new version and allows rollback when the release is unhealthy.

---

# 18. Kafka Is Part of Runtime

```text
User
 ↓
Payment Service
 ↓
Kafka
 ↓
Consumer
```

Use Kafka when:

```text
Immediate response is not required
Loose coupling is useful
Replay is useful
Multiple consumers need the same event
```

---

# 19. Prometheus and Grafana Are Observability

```text
Payment Service
     │
     └── metrics
           ↓
       Prometheus
           ↓
        Grafana
```

> Prometheus collects metrics. Grafana visualizes them.

---

# 20. Jenkins Is CI/CD

```text
Git
 ↓
Jenkins
 ├── mvn test
 ├── mvn package
 ├── docker build
 ├── docker push
 └── kubectl deploy
```

> **Interview answer:** Jenkins is the CI/CD orchestrator. It builds, tests, packages, publishes, deploys, and verifies the application.

---

# 21. Moving the SAME App to GCP

Only the platform changes.

```text
LOCAL                          GCP

Local Docker Image      →      Artifact Registry
Minikube                →      GKE
Minikube Service URL    →      Cloud Load Balancer IP
Local Prometheus        →      Managed Prometheus option
Local logs              →      Cloud Logging option
Kubernetes Secret       →      Secret Manager option
H2 lab database         →      Persistent DB such as Cloud SQL
```

---

# 22. Full GCP Mental Model

```text
Developer
   ↓
Git
   ↓
Jenkins
   ↓
Maven Test + Package
   ↓
Docker Build
   ↓
Docker Image
   ↓
Artifact Registry
   ↓
GKE
   ↓
Deployment
   ↓
ReplicaSet
   ↓
Pod
   ↓
Container
   ↓
Payment Service
   ↓
Kubernetes Service
   ↓
Google Cloud Load Balancer
   ↓
User
```

Side flows:

```text
Payment Service → Kafka → Consumers
Payment Service → Database
Payment Service → Metrics → Managed Prometheus / Cloud Monitoring
Application logs → Cloud Logging
```

---

# 23. GCP — One-Time Setup

```bash
gcloud auth login
gcloud config set project <PROJECT_ID>

gcloud services enable container.googleapis.com
gcloud services enable artifactregistry.googleapis.com
```

---

# 24. Artifact Registry

Create:

```bash
gcloud artifacts repositories create payment-repo ^
  --repository-format=docker ^
  --location=asia-south1
```

Configure Docker:

```bash
gcloud auth configure-docker asia-south1-docker.pkg.dev
```

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

---

# 25. GKE

Create a small learning cluster:

```bash
gcloud container clusters create payment-cluster ^
  --zone=asia-south1-a ^
  --num-nodes=1
```

Connect:

```bash
gcloud container clusters get-credentials payment-cluster ^
  --zone=asia-south1-a
```

Verify:

```bash
kubectl get nodes
```

> **Memory:** Minikube teaches Kubernetes locally. GKE runs Kubernetes in Google Cloud.

---

# 26. Deploy to GKE

Use the Artifact Registry image:

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

# 27. Expose the App in GCP

Use:

```yaml
type: LoadBalancer
```

Then:

```bash
kubectl get svc payment-service
```

Flow:

```text
Internet → Google Cloud Load Balancer → Kubernetes Service → Ready Pods
```

---

# 28. Jenkins → Artifact Registry → GKE

```text
Git
 ↓
Jenkins
 ↓
Unit Tests
 ↓
Maven Package
 ↓
Docker Build
 ↓
Artifact Registry
 ↓
GKE Deployment
 ↓
Rollout Check
 ↓
Health Check
```

Deploy a new image:

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

---

# 29. Jenkins Authentication in GCP

Manual learning:

```text
gcloud auth login
```

Production idea:

```text
Jenkins
 ↓
Machine / workload identity
 ↓
IAM
 ↓
Artifact Registry + GKE
```

> **Interview crux:** Authentication tells GCP who Jenkins is. IAM decides what Jenkins can do.

Use least privilege.

---

# 30. Secrets in GCP

Lab:

```text
Kubernetes Secret
```

Production-style GCP:

```text
GKE Pod
 ↓
Workload Identity
 ↓
Secret Manager
```

> Avoid long-lived cloud credentials inside the image.

---

# 31. GCP Observability

```text
Payment Service
     ├── metrics → Managed Prometheus / Cloud Monitoring
     └── logs    → Cloud Logging
```

You can still use:

```bash
kubectl logs <pod>
```

for immediate Pod troubleshooting.

---

# 32. Local vs GCP — Final Comparison

```text
LOCAL

Git → Jenkins → Maven → Docker Image → Local Docker → Minikube
    → Deployment → Pod → Service → User
```

```text
GCP

Git → Jenkins → Maven → Docker Image → Artifact Registry → GKE
    → Deployment → Pod → Service → Cloud Load Balancer → User
```

The Kubernetes concepts remain the same.

---

# 33. Interview Questions You Must Answer

## Docker vs Kubernetes?

> Docker packages and runs containers. Kubernetes manages containerized applications at scale, including scheduling, self-healing, scaling, networking, and rollout.

## Why Jenkins?

> Jenkins automates the CI/CD flow from source code to tested, packaged, deployed application.

## Why Artifact Registry?

> It stores versioned Docker images so Jenkins can publish them and GKE can pull the exact release image.

## Minikube vs GKE?

> Minikube is local Kubernetes for learning and development. GKE is managed Kubernetes on Google Cloud.

## Deployment vs Pod?

> A Pod runs the container. A Deployment manages the desired number and version of Pods.

## Why Service?

> Pod IPs are temporary. A Service gives stable network access to selected Pods.

## Liveness vs Readiness?

> Liveness decides restart. Readiness decides whether traffic should reach the Pod.

## How does Jenkins deploy to GKE?

> Jenkins builds and pushes the image to Artifact Registry, authenticates to GCP, updates the Kubernetes Deployment, and verifies the rollout.

## How do you troubleshoot a failing Pod?

> I check status, events with `describe`, current or previous logs, configuration, probes, dependencies, resources, Service endpoints, and application health.

## Why might 10 Pods not be faster than 3?

> Because the real bottleneck may be the database, Kafka, connection pool, external API, CPU limit, or another shared dependency.

---

# 34. One Architect-Level Answer

### Explain your Payment App CI/CD and runtime architecture.

> A developer pushes code to Git. Jenkins runs tests and builds the Spring Boot JAR with Maven. Jenkins creates an immutable Docker image and stores it in the image registry. Kubernetes runs that image through a Deployment, ReplicaSet, and Pods. A Service gives stable access to the Pods. Kafka handles asynchronous events. Prometheus and Grafana provide metrics and dashboards. Locally, the cluster is Minikube. In GCP, Artifact Registry stores the image and GKE runs the Kubernetes workload, with a cloud load balancer exposing the Service.

---

# 35. Commands to Remember by PURPOSE

## Docker

```text
SEE      → docker ps
LOG      → docker logs
ENTER    → docker exec
CONTROL  → docker start / stop
```

## Kubernetes

```text
SEE        → kubectl get
UNDERSTAND → kubectl describe
LOG        → kubectl logs
SCALE      → kubectl scale
DEPLOY     → kubectl rollout
```

## GCP

```text
LOGIN   → gcloud auth login
SELECT  → gcloud config set project
CONNECT → gcloud container clusters get-credentials
STORE   → Artifact Registry
RUN     → GKE
```

---

# 36. Best Learning Method

```text
UNDERSTAND
   ↓
RUN
   ↓
BREAK
   ↓
OBSERVE
   ↓
FIX
   ↓
EXPLAIN
```

Practice:

```text
Delete a Pod → watch self-healing
Scale 1 → 3 → watch replicas
Break Service selector → find missing endpoints
Use bad image tag → see ImagePullBackOff
Break app config → inspect CrashLoopBackOff
Deploy version 2 → verify rollout
Rollback → confirm version 1 returns
```

---


# 37. Kubernetes Control Plane — Know the Picture, Not Internals

You do not need cluster-admin depth, but an architect should know who makes Kubernetes decisions.

```text
kubectl
  ↓
API Server
  ├── Scheduler          → chooses a Node for a Pod
  ├── Controller Manager → keeps actual state = desired state
  └── etcd               → stores cluster state

Worker Node
  ↓
kubelet
  ↓
Pod
  ↓
Container
```

> **Interview crux:** The API Server is the front door, the Scheduler chooses placement, controllers maintain desired state, etcd stores cluster state, and kubelet runs Pods on each Node.

---

# 38. Requests, Limits, and OOMKilled

```text
Request = what the Pod needs for scheduling
Limit   = maximum resource it may consume
```

Example:

```yaml
resources:
  requests:
    cpu: "250m"
    memory: "256Mi"
  limits:
    cpu: "1"
    memory: "512Mi"
```

If the container exceeds its memory limit, it may be terminated with:

```text
OOMKilled
```

Check:

```bash
kubectl describe pod <pod>
```

> **Interview crux:** Requests help scheduling and capacity planning. Limits protect the cluster, but badly chosen limits can also hurt the application.

---

# 39. HPA — Automatic Pod Scaling

Manual scaling:

```bash
kubectl scale deployment payment-service --replicas=3
```

Automatic scaling:

```text
Metric rises
   ↓
HPA increases replicas
   ↓
More Pods
```

> **Interview crux:** HPA changes Pod replica count based on metrics. It does not remove downstream bottlenecks.

Do not confuse:

```text
HPA = more/fewer Pods
Node autoscaling = more/fewer Nodes
```

---

# 40. Service vs Ingress vs Load Balancer

```text
Pod
 ↑
Service
 ↑
Ingress / Gateway
 ↑
External Load Balancer
 ↑
User
```

### Service

Stable access to Pods.

### LoadBalancer Service

Asks the cloud provider for an external load balancer for that Service.

### Ingress / Gateway

Routes HTTP traffic to multiple Services using rules such as host or path.

Example:

```text
/payments → payment-service
/orders   → order-service
```

> **Interview crux:** Service finds Pods. Ingress or Gateway routes external HTTP traffic between Services.

---

# 41. Namespace

Namespace gives logical separation inside one Kubernetes cluster.

```text
Cluster
 ├── dev
 ├── test
 └── prod
```

It can help separate:

```text
Names
Access
Policies
Quotas
```

> **Interview crux:** Namespace is logical isolation, not a complete security boundary by itself.

---

# 42. Persistent Storage — PV and PVC

Pods are disposable.

Do not store important payment data only inside a Pod filesystem.

```text
Pod
 ↓
PVC = storage request
 ↓
PV = storage provided to the workload
```

> **Interview crux:** Compute is disposable; business data must survive Pod replacement.

For your lab:

```text
H2 in-memory DB → good for learning
```

For a real payment platform:

```text
External persistent database → preferred
```

---

# 43. Deployment vs StatefulSet

### Deployment

Best for stateless services.

```text
payment-service
notification-service
API service
```

Pods are replaceable.

### StatefulSet

Use when Pods need stable identity or stable attached storage.

Typical examples:

```text
Kafka brokers
Some databases
Stateful clustered software
```

> **Interview crux:** Deployment manages interchangeable Pods. StatefulSet manages Pods that need stable identity or storage.

---

# 44. ServiceAccount and RBAC

Two different questions:

```text
Who is the Pod?
        ↓
ServiceAccount

What may it do inside Kubernetes?
        ↓
RBAC
```

RBAC uses:

```text
Role / ClusterRole
        +
RoleBinding / ClusterRoleBinding
```

> **Interview crux:** ServiceAccount gives workload identity inside Kubernetes; RBAC controls Kubernetes API permissions.

In GCP, also distinguish:

```text
Kubernetes identity
        ↓
Workload Identity Federation for GKE
        ↓
Google Cloud IAM
        ↓
GCP resource
```

---

# 45. Rolling Update — What Controls Availability?

Deployment commonly uses a rolling strategy.

Two important settings:

```text
maxSurge
= how many extra Pods may temporarily exist

maxUnavailable
= how many desired Pods may temporarily be unavailable
```

Mental model:

```text
Old Pods
   ↓
New Pods become Ready
   ↓
Old Pods are removed
```

> **Interview crux:** A safe rollout depends on readiness checks and rollout strategy, not only on creating new Pods.

---

# 46. Ten Interview Traps to Avoid

### 1. Docker Image vs Container

```text
Image = package
Container = running instance
```

### 2. Pod vs Container

```text
Pod contains one or more containers.
```

### 3. Pod vs Deployment

```text
Pod runs.
Deployment manages Pods.
```

### 4. Service vs Deployment

```text
Deployment manages lifecycle.
Service manages stable network access.
```

### 5. Service vs Ingress

```text
Service routes to Pods.
Ingress/Gateway routes HTTP traffic to Services.
```

### 6. Liveness vs Readiness

```text
Liveness → restart
Readiness → traffic
```

### 7. Request vs Limit

```text
Request → scheduling expectation
Limit → maximum resource usage
```

### 8. HPA vs Node Autoscaling

```text
HPA → Pods
Node autoscaling → Nodes
```

### 9. Jenkins vs Kubernetes

```text
Jenkins deploys.
Kubernetes runs.
```

### 10. Minikube vs GKE

```text
Minikube → local Kubernetes
GKE      → managed Kubernetes on GCP
```

---

# 47. Final Architect Troubleshooting Model

When production is slow or unavailable, think in layers:

```text
USER
 ↓
LOAD BALANCER / INGRESS
 ↓
SERVICE
 ↓
ENDPOINTS
 ↓
POD
 ↓
CONTAINER
 ↓
SPRING BOOT
 ↓
DATABASE / KAFKA / EXTERNAL API
```

Then ask:

```text
Can traffic enter?
Can Service find ready Pods?
Are Pods healthy?
Is the JVM healthy?
Are resources exhausted?
Are dependencies healthy?
Is the bottleneck outside Kubernetes?
```

> **Architect crux:** Kubernetes can keep Pods alive, but it cannot fix a slow database, broken business logic, bad SQL, Kafka lag, or an unavailable downstream system.

---

# 48. The One Interview Story

If an interviewer says:

> "Explain how your application goes from code to production."

Answer in this order:

```text
1. Developer commits code to Git.
2. Jenkins starts the CI/CD pipeline.
3. Maven tests and packages the Spring Boot application.
4. Docker creates an immutable image.
5. The image is stored in a registry.
6. Kubernetes Deployment references that image.
7. ReplicaSet maintains the requested Pods.
8. Each Pod runs the application container.
9. Readiness decides whether the Pod receives traffic.
10. Service gives stable access to ready Pods.
11. In cloud, a Load Balancer or Ingress exposes the Service.
12. Kafka handles asynchronous events.
13. ConfigMap and Secret provide runtime configuration.
14. Prometheus/Grafana or cloud monitoring observe the application.
15. If a Pod dies, Kubernetes replaces it.
16. HPA can add Pods when load rises.
17. If a release fails, Deployment can roll it back.
```

This is the full mental model in interview order.

---

# 49. Final Memory Line


> **Git stores → Jenkins builds → Maven packages → Docker creates the image → Registry stores it → Kubernetes runs it → Deployment maintains Pods → Service exposes ready Pods → Ingress/Load Balancer accepts external traffic → Kafka connects async work → ConfigMap/Secret configure it → Prometheus/Grafana observe it → GCP replaces local image storage with Artifact Registry and Minikube with GKE.**

If this line is clear, the architecture has no missing link.
