
# PAYMENT APP — COMPLETE STARTUP GUIDE

## ⚡ Quick Options to Run

| Environment | Tools Required | Commands |
| :--- | :--- | :--- |
| **Local Docker Compose** | Docker Desktop | `cd docker && docker compose up -d` then `mvn spring-boot:run` |
| **Local Minikube** | Docker + Minikube | `minikube start` then `kubectl apply -f k8s/` |
| **Production GCP GKE** | Terraform + Helm | `cd terraform && terraform apply` then `helm upgrade --install ...` |

---

### Option A — Fastest Local Run (Docker Compose Only, No Minikube/Helm Needed)

1. Start Kafka and Kafka UI:
   ```bash
   cd docker
   docker compose up -d
   ```
2. Run the application:
   ```bash
   cd ..
   ./mvnw spring-boot:run
   ```
3. Test payment endpoint:
   ```bash
   curl -X POST http://localhost:8080/api/v1/payments \
     -H "Content-Type: application/json" \
     -H "Idempotency-Key: TEST-001" \
     -d '{"fromAccount":"ACC-001","toAccount":"ACC-002","amount":500.00,"currency":"EUR","standard":"ISO_20022"}'
   ```
4. Access Kafka UI: `http://localhost:8090`

---

### Option B — Local Kubernetes with Minikube

**Follow every step carefully each time you start your computer**

---

# STEP 1 — START DOCKER DESKTOP

1. Click **Windows Start**
2. Search **Docker Desktop**
3. Open it
4. Wait until:

   * Bottom-left icon becomes **GREEN**
   * Message shows: **Docker Desktop is running**

⏱ Takes around **1–2 minutes**

⚠️ **Do NOT continue until Docker becomes green**

---

# STEP 2 — OPEN COMMAND PROMPT

1. Click **Start**
2. Search **cmd**
3. Right click → **Run as administrator**
4. Click **Yes**

Keep this window open.

---

# STEP 3 — START MINIKUBE

Run:

```bash
minikube start --driver=docker
```

Wait until you see:

```text
Done! kubectl is now configured to use "minikube"
```

Verify:

```bash
kubectl get nodes
```

Expected:

```text
NAME       STATUS   ROLES           AGE   VERSION
minikube   Ready    control-plane   1m    v1.29.0
```

✅ STATUS should be **Ready**

If **NotReady** → wait 1 minute and retry.

---

# STEP 4 — START JENKINS

Run:

```bash
docker start elated_swirles
```

Expected:

```text
elated_swirles
```

Wait **30 seconds**

Open:

```text
http://localhost:8888
```

Login:

```text
Username: admin
Password: (your configured password)
```

---

# STEP 5 — FIX DOCKER PERMISSION FOR JENKINS

Run:

```bash
docker exec -u root elated_swirles chmod 666 /var/run/docker.sock
```

No output = success.

---

# STEP 6 — COPY MINIKUBE CERTIFICATES INTO JENKINS

Run:

```bash
docker cp C:\Users\Admin\.minikube\ca.crt elated_swirles:/var/jenkins_home/.minikube/ca.crt
```

```bash
docker cp C:\Users\Admin\.minikube\profiles\minikube\client.crt elated_swirles:/var/jenkins_home/.minikube/profiles/minikube/client.crt
```

```bash
docker cp C:\Users\Admin\.minikube\profiles\minikube\client.key elated_swirles:/var/jenkins_home/.minikube/profiles/minikube/client.key
```

Verify:

```bash
docker exec elated_swirles ls /var/jenkins_home/.minikube/profiles/minikube/
```

Expected:

```text
client.crt
client.key
```

---

# STEP 7 — GET MINIKUBE IP

Run:

```bash
minikube ip
```

Example:

```text
192.168.49.2
```

Write this IP down.

---

# STEP 8 — UPDATE KUBERNETES CONNECTION IN JENKINS

Replace with your Minikube IP:

```bash
docker exec elated_swirles bash -c "sed -i 's|https://[0-9.]*:8443|https://192.168.49.2:8443|g' /var/jenkins_home/.kube/config"
```

Verify:

```bash
docker exec elated_swirles kubectl get nodes
```

Expected:

```text
NAME       STATUS
minikube   Ready
```

---

# STEP 9 — VERIFY PODS

Run:

```bash
kubectl get pods
```

Expected:

```text
STATUS = Running
```

If:

### Pending / ContainerCreating

Wait 2 minutes.

### CrashLoopBackOff

Run:

```bash
kubectl delete pods --all
```

Wait 3 minutes.

---

# STEP 10 — GET SERVICE URLS

Payment:

```bash
minikube service payment-service --url
```

Prometheus:

```bash
minikube service prometheus --url
```

Grafana:

```bash
minikube service grafana --url
```

Example:

```text
Payment → http://192.168.49.2:31234
Prometheus → http://192.168.49.2:30090
Grafana → http://192.168.49.2:30030
```

---

# STEP 11 — VERIFY PAYMENT APP

Open:

```text
http://<PAYMENT_URL>/actuator/health
```

Expected:

```json
{"status":"UP"}
```

---

# STEP 12 — VERIFY PROMETHEUS

Open:

```text
http://<PROMETHEUS_URL>
```

Navigate:

```text
Status → Targets
```

Verify:

```text
payment-service → UP
```

---

# STEP 13 — VERIFY GRAFANA

Open:

```text
http://<GRAFANA_URL>
```

Login:

```text
Username: admin
Password: admin123
```

Navigate:

```text
Dashboards
→ Payment Service
→ Payment Service Dashboard
```

---

# QUICK LINKS

```text
Payment Health:
http://<IP>:31234/actuator/health

Payment API:
http://<IP>:31234/api/v1/payments

H2 Console:
http://<IP>:31234/h2-console

Prometheus:
http://<IP>:30090

Grafana:
http://<IP>:30030

Jenkins:
http://localhost:8888

Kafka UI:
http://localhost:8090
```

---

# H2 LOGIN

```text
JDBC:
jdbc:h2:mem:paymentdb

Username:
sa

Password:
(blank)
```

---

# TEST PAYMENT

Create:

```bash
curl -X POST http://<PAYMENT_URL>/api/v1/payments \
-H "Content-Type: application/json" \
-d "{\"fromAccount\":\"ACC-001\",\"toAccount\":\"ACC-002\",\"amount\":500.00,\"currency\":\"EUR\",\"standard\":\"ISO_20022\"}"
```

Get all:

```bash
curl http://<PAYMENT_URL>/api/v1/payments
```

Process:

```bash
curl -X POST http://<PAYMENT_URL>/api/v1/payments/{id}/process
```

---

# COMPLETE CHECKLIST

```text
[ ] Docker running
[ ] CMD opened as admin
[ ] Minikube Ready
[ ] Jenkins accessible
[ ] Docker permission fixed
[ ] Certificates copied
[ ] Minikube IP noted
[ ] Jenkins Kubeconfig fixed
[ ] Pods Running
[ ] URLs collected
[ ] Health endpoint UP
[ ] Prometheus UP
[ ] Grafana Dashboard visible
```

# APPLICATION READY ✅
