# Payment Service

A production-grade payment processing service built with **Spring Boot 3.x**, supporting **ISO 8583** (card/POS) and **ISO 20022** (SEPA/bank transfer) message standards. Fully containerized with Docker, deployed on Kubernetes, monitored with Prometheus and Grafana, and automated via Jenkins CI/CD.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.2.5 |
| Messaging | Apache Kafka |
| Database | H2 (in-memory, local dev) |
| Containerization | Docker |
| Orchestration | Kubernetes (Minikube for local) |
| CI/CD | Jenkins |
| Monitoring | Prometheus + Grafana |
| Registry | Docker Hub |

---

## Project Structure

```
payment-app/
├── src/
│   ├── main/java/com/payment/
│   │   ├── PaymentServiceApplication.java
│   │   ├── controller/
│   │   │   └── PaymentController.java
│   │   ├── service/
│   │   │   └── PaymentService.java
│   │   ├── model/
│   │   │   ├── Payment.java
│   │   │   └── PaymentDTO.java
│   │   ├── repository/
│   │   │   └── PaymentRepository.java
│   │   ├── kafka/
│   │   │   ├── PaymentProducer.java
│   │   │   └── PaymentConsumer.java
│   │   └── config/
│   │       ├── KafkaTopicConfig.java
│   │       ├── PaymentMetrics.java
│   │       └── GlobalExceptionHandler.java
│   ├── main/resources/
│   │   └── application.yml
│   └── test/java/com/payment/
│       └── PaymentServiceIntegrationTest.java
├── docker/
│   └── docker-compose.yml          # Kafka + Zookeeper + Kafka UI
├── k8s/
│   ├── deployment.yaml             # App Deployment + Service + HPA
│   ├── kafka.yaml                  # Kafka + Zookeeper in Kubernetes
│   ├── prometheus.yaml             # Prometheus Deployment + ConfigMap
│   ├── grafana.yaml                # Grafana Deployment + Dashboard
│   ├── ingress.yaml                # Ingress for domain name routing
│   └── secrets.yaml                # Kubernetes Secrets
├── jenkins/
│   └── Jenkinsfile                 # CI/CD Pipeline
├── Dockerfile                      # Multi-stage Docker build
└── pom.xml
```

---

## Payment Standards

| Standard | Used For | Reference ID |
|----------|----------|-------------|
| ISO 8583 | Card / POS transactions | `POS-XXXXXXXX` |
| ISO 20022 | SEPA / Bank transfers | `SEPA-XXXXXXXX` |

---

## API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/payments` | Create a new payment |
| GET | `/api/v1/payments` | Get all payments |
| GET | `/api/v1/payments/{id}` | Get payment by ID |
| GET | `/api/v1/payments/account/{acc}` | Get payments by account |
| POST | `/api/v1/payments/{id}/process` | Process payment (PENDING → COMPLETED) |
| POST | `/api/v1/payments/{id}/reverse` | Reverse payment (COMPLETED → REVERSED) |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/prometheus` | Prometheus metrics endpoint |

---

## Payment Status Flow

```
PENDING → PROCESSING → COMPLETED → REVERSED
                    ↘ FAILED
```

---

## Kafka Topics

| Topic | Published When |
|-------|---------------|
| `payment.initiated` | Payment created |
| `payment.processed` | Payment completed |
| `payment.failed` | Payment reversed or failed |

---

## Metrics (Prometheus + Grafana)

| Metric | Type | Description |
|--------|------|-------------|
| `payment_created_total` | Counter | Total payments created |
| `payment_standard_total` | Counter | Payments by ISO standard |
| `payment_status_total` | Counter | Payments by status |
| `payment_pending_count` | Gauge | Live count of pending payments |
| `payment_processing_duration` | Timer | Processing time p50/p95/p99 |
| `http_server_requests_seconds` | Timer | HTTP request latency |

---

## Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 17+ | https://adoptium.net |
| Maven | 3.9+ | https://maven.apache.org |
| Docker Desktop | Latest | https://docker.com/products/docker-desktop |
| Minikube | Latest | https://minikube.sigs.k8s.io |
| kubectl | Latest | https://kubernetes.io/docs/tasks/tools |
| Git | Any | https://git-scm.com |

---

## Local Development Setup

### Step 1 — Clone the repository

```bash
git clone https://github.com/purushotentitle/payment-app.git
cd payment-app
```

### Step 2 — Start Kafka locally

```bash
cd docker
docker-compose up -d
```

### Step 3 — Run the application

```bash
mvn spring-boot:run
```

Application starts at `http://localhost:8080`

### Step 4 — Run tests

```bash
mvn test
```

Tests use embedded Kafka. No external dependencies needed.

---

## Local Development URLs

| Service | URL |
|---------|-----|
| Payment API | http://localhost:8080/api/v1/payments |
| Health Check | http://localhost:8080/actuator/health |
| H2 Console | http://localhost:8080/h2-console |
| Kafka UI | http://localhost:8090 |

**H2 Console login:**
```
JDBC URL  : jdbc:h2:mem:paymentdb
Username  : sa
Password  : (leave blank)
```

---

## Kubernetes Deployment (Minikube)

### Step 1 — Start Minikube

```bash
minikube start --driver=docker
minikube addons enable ingress
```

### Step 2 — Add domain names to hosts file

Get minikube IP:
```bash
minikube ip
```

Add to `C:\Windows\System32\drivers\etc\hosts` (Windows) or `/etc/hosts` (Mac/Linux).
Replace `192.168.49.2` with your actual minikube IP:

```
192.168.49.2    payment.local
192.168.49.2    prometheus.local
192.168.49.2    grafana.local
```

> **Note:** Minikube IP changes every restart. Update the hosts file each time.

### Step 3 — Deploy all services

```bash
kubectl apply -f k8s/
```

### Step 4 — Verify all pods are running

```bash
kubectl get pods
```

Expected output:
```
NAME                          READY   STATUS
zookeeper-xxx                 1/1     Running
kafka-xxx                     1/1     Running
payment-service-xxx           1/1     Running
payment-service-xxx           1/1     Running
prometheus-xxx                1/1     Running
grafana-xxx                   1/1     Running
```

---

## Kubernetes URLs

| Service | URL |
|---------|-----|
| Payment API | http://payment.local/api/v1/payments |
| Health Check | http://payment.local/actuator/health |
| Prometheus | http://prometheus.local |
| Grafana | http://grafana.local |
| Jenkins | http://localhost:8888 |

> **H2 Console** must be accessed via port-forward — not via ingress:

```bash
# Step 1 - Get pod name
kubectl get pods

# Step 2 - Port forward (replace pod name with actual name)
kubectl port-forward pod/payment-service-56f7d7c45c-jm5xj 8080:8080

# Step 3 - Open in browser
# http://localhost:8080/h2-console
```

**Grafana login:**
```
Username : admin
Password : admin123
```

---

## CI/CD Pipeline (Jenkins)

The `jenkins/Jenkinsfile` defines a 6-stage pipeline:

```
Checkout → Build JAR → Run Tests → Build Docker Image → Push to Registry → Deploy to Kubernetes
```

| Stage | Description |
|-------|-------------|
| Checkout | Pull latest code from GitHub |
| Build JAR | Maven clean package |
| Run Tests | Unit and integration tests with embedded Kafka |
| Build Docker Image | Multi-stage Docker build |
| Push to Registry | Push image to Docker Hub |
| Deploy to Kubernetes | kubectl apply + rollout status check |

### Jenkins requirements

- Jenkins running at `http://localhost:8888`
- Docker installed inside Jenkins container
- kubectl installed inside Jenkins container
- Credential ID `docker-registry-creds` configured with Docker Hub login
- GitHub repository connected as SCM source

---

## Docker

### Build image

```bash
docker build -t purushotentitle/payment-service:1.0.0 .
```

### Run container

```bash
docker run -p 8080:8080 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  purushotentitle/payment-service:1.0.0
```

---

## Sample API Requests

### Create ISO 20022 Payment (SEPA Bank Transfer)

```bash
curl -X POST http://payment.local/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "DE89370400440532013000",
    "toAccount":   "GB29NWBK60161331926819",
    "amount":      1500.00,
    "currency":    "EUR",
    "standard":    "ISO_20022",
    "description": "Invoice payment Q2"
  }'
```

### Create ISO 8583 Payment (Card/POS Transaction)

```bash
curl -X POST http://payment.local/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "4111111111111111",
    "toAccount":   "MERCHANT-001",
    "amount":      49.99,
    "currency":    "USD",
    "standard":    "ISO_8583",
    "description": "POS transaction"
  }'
```

### Process a Payment

```bash
curl -X POST http://payment.local/api/v1/payments/{id}/process
```

### Reverse a Payment

```bash
curl -X POST http://payment.local/api/v1/payments/{id}/reverse
```

### Get All Payments

```bash
curl http://payment.local/api/v1/payments
```

---

## Autoscaling

The HPA automatically scales the payment service based on CPU usage:

```
Min replicas : 2
Max replicas : 10
CPU target   : 70%
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Pods in CrashLoopBackOff | `kubectl delete pods --all` then wait 3 minutes |
| `payment.local` not reachable | Run `minikube ip` and update hosts file with new IP |
| H2 Console remote connection error | Use port-forward method — never access via ingress |
| Jenkins Docker permission denied | `docker exec -u root elated_swirles chmod 666 /var/run/docker.sock` |
| Jenkins kubectl not connecting | Recopy minikube certs (Steps 6-8 in startup guide) |
| Ingress ADDRESS column empty | Wait 2 minutes then run `kubectl get ingress` again |

---

## Author

**Raj** — Payment Platform Engineer
