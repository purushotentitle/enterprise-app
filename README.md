# Payment Processing Platform — GCP GKE Production & Local Runbook

Enterprise-grade payment microservice supporting **ISO 8583** (Card/POS) and **ISO 20022** (SEPA credit transfer) with Kafka event bus, PostgreSQL with Flyway migrations, distributed idempotency, and GCP GKE High-Availability infrastructure via Terraform and Helm.

---

## 🚀 Running on Your Local System

You do **NOT** need Terraform or Helm to run this application on your local machine. You have two options:

### Option 1: Local Docker Compose (Fastest — No Minikube or Helm Needed)

1. **Start Kafka and Kafka UI:**
   ```bash
   cd payment-app/docker
   docker compose up -d
   ```
   - Kafka Broker: `localhost:9092`
   - Kafka Web UI: `http://localhost:8090`

2. **Run Spring Boot Application:**
   ```bash
   cd ..
   ./mvnw spring-boot:run
   # Or: mvn spring-boot:run
   ```
   - API Endpoint: `http://localhost:8080/api/v1/payments`
   - Health Probes: `http://localhost:8080/actuator/health`

3. **Test Payment Creation (with Idempotency-Key):**
   ```bash
   curl -X POST http://localhost:8080/api/v1/payments \
     -H "Content-Type: application/json" \
     -H "Idempotency-Key: LOCAL-TX-001" \
     -d '{
       "fromAccount": "NL91ABNA0417164300",
       "toAccount": "DE89370400440532013000",
       "amount": 250.00,
       "currency": "EUR",
       "standard": "ISO_20022",
       "description": "Local test transfer"
     }'
   ```

---

### Option 2: Local Kubernetes via Minikube

1. **Start Minikube:**
   ```bash
   minikube start --driver=docker --cpus=4 --memory=8192
   ```

2. **Build Image in Minikube's Docker Environment:**
   ```bash
   eval $(minikube docker-env)
   docker build -t payment-app:1.0.0 ./payment-app
   ```

3. **Deploy using Kubernetes Manifests (No Helm needed):**
   ```bash
   kubectl apply -f payment-app/k8s/
   ```

4. **Verify Deployment:**
   ```bash
   kubectl get pods -l app=payment-service
   minikube service payment-service --url
   ```

---

## ☁️ Production Deployment on GCP GKE

For production environments, Terraform provisions the multi-AZ infrastructure, and Helm packages the workloads following High-Availability (HA) best practices.

### 1. Provision GCP Infrastructure via Terraform

```bash
cd terraform

# 1. Initialize Terraform
terraform init

# 2. Plan and inspect resources
terraform plan -var="project_id=YOUR_PROJECT_ID" -var="region=us-central1" -out=tfplan

# 3. Apply infrastructure
terraform apply tfplan
```

**Resources Provisioned:**
- **VPC & Cloud NAT**: Custom VPC, secondary Pod/Service CIDRs, and Cloud NAT for private egress.
- **Regional GKE Cluster**: 3 Availability Zones (`us-central1-a`, `b`, `c`), Dataplane V2 (Cilium), private nodes, Workload Identity.
- **Cloud SQL PostgreSQL HA**: Synchronous active-standby replication with sub-minute automated failover.
- **Cloud Armor WAF**: DDoS defense, 120 req/min payment rate limiting, and OWASP Core Rules.
- **Artifact Registry**: Private container image repository.

### 2. Build & Push Production Container Image

```bash
# Configure Docker authentication
gcloud auth configure-docker us-central1-docker.pkg.dev

# Build and push to Artifact Registry
docker build -t us-central1-docker.pkg.dev/YOUR_PROJECT_ID/prod-payment-app-repo/payment-app:1.0.0 ./payment-app
docker push us-central1-docker.pkg.dev/YOUR_PROJECT_ID/prod-payment-app-repo/payment-app:1.0.0
```

### 3. Deploy via Production Helm Chart

```bash
# Connect kubectl to regional GKE
gcloud container clusters get-credentials prod-payment-app-cluster --region us-central1 --project YOUR_PROJECT_ID

# Deploy Helm release with production HA overrides
helm upgrade --install payment-app ./helm/payment-app \
  --namespace payment-system \
  --create-namespace \
  -f ./helm/payment-app/values-prod.yaml \
  --set image.repository="us-central1-docker.pkg.dev/YOUR_PROJECT_ID/prod-payment-app-repo/payment-app" \
  --set image.tag="1.0.0"
```

---

## 🛡️ Production High-Availability Checklist

| Feature | Implementation | Purpose |
| :--- | :--- | :--- |
| **Multi-AZ Pod Anti-Affinity** | `podAntiAffinity.requiredDuringScheduling` | Strict guarantee that pods are never colocated on the same node |
| **Topology Spread Constraints** | `topologyKey: topology.kubernetes.io/zone` (`maxSkew: 1`) | Uniform distribution of replicas across all 3 GCP zones |
| **Pod Disruption Budget** | `minAvailable: 2` (or `3` in prod) | GKE node upgrades or maintenance will never evict more than 1 pod |
| **Zero-Downtime Rollouts** | `maxSurge: 25%`, `maxUnavailable: 0` | New pods pass readiness checks before old pods terminate |
| **Graceful Shutdown** | `preStop: sleep 15`, `terminationGracePeriod: 60s` | Permits active in-flight payment transactions to finalize |
| **Distributed Idempotency** | `Idempotency-Key` header + DB check | Guarantees zero duplicate charges during network timeouts |
| **Database Migrations** | Flyway (`V1`, `V2`) + `hibernate.ddl-auto: validate` | Immutable, version-controlled schema evolution |
| **Distributed Tracing** | `X-Correlation-ID` + SLF4J MDC + Kafka headers | End-to-end transaction path tracing across asynchronous hops |
