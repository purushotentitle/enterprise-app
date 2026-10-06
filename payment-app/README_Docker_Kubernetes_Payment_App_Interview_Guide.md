# Docker + Kubernetes — Interview-Ready Payment App Guide

Use your existing Payment App to learn by **running, breaking, observing, and fixing**.

---

## 1. One Mental Model

```text
Java Code
   ↓
Maven Build
   ↓
Docker Image
   ↓
Container
   ↓
Kubernetes Deployment
   ↓
ReplicaSet
   ↓
Pod
   ↓
Service
   ↓
Payment API
```

Around it:

```text
Git → Jenkins → Docker → Kubernetes
Payment Service → Kafka → Consumers
Payment Service → Prometheus → Grafana
```

> **Interview crux:** Docker packages the application. Kubernetes runs, heals, scales, and exposes it.

---

## 2. Docker — Remember 5 Questions

```text
What images exist?
What containers are running?
Why did one fail?
How do I enter it?
How do I control it?
```

```bash
docker images
docker ps
docker logs <container>
docker exec -it <container> bash
docker start <container>
docker stop <container>
```

> **Image = packaged blueprint. Container = running instance.**

Easy memory:

```text
Java Class ≈ Docker Image
Java Object ≈ Docker Container
```

---

## 3. Kubernetes — Remember 7 Questions

```text
What is running?
Where is it running?
Why is it failing?
How is traffic reaching it?
What configuration is it using?
Can Kubernetes recover it?
Can Kubernetes scale it?
```

Core commands:

```bash
kubectl get pods
kubectl get pods -o wide
kubectl describe pod <pod>
kubectl logs <pod>
kubectl get svc
kubectl get configmap
kubectl scale deployment <deployment> --replicas=3
```

---

## 4. Deployment → ReplicaSet → Pod

```text
Deployment
   ↓
ReplicaSet
   ↓
Pods
   ↓
Containers
```

> **Interview crux:** Deployment defines desired state. Kubernetes continuously makes actual state match desired state.

Example:

```text
Desired Pods = 3
Actual Pods  = 2

Kubernetes creates 1 more.
```

---

## 5. Self-Healing

```bash
kubectl get pods
kubectl delete pod <payment-pod>
kubectl get pods -w
```

> **Interview answer:** If a Pod disappears, the Deployment controller notices that actual state no longer matches desired state and creates a replacement.

---

## 6. Scaling

```bash
kubectl scale deployment payment-service --replicas=3
kubectl get pods
```

Return to one:

```bash
kubectl scale deployment payment-service --replicas=1
```

> **Architect point:** More Pods help only if shared dependencies such as the database and Kafka can also handle the load.

---

## 7. Service — Stable Access to Changing Pods

```text
                  Pod 1
                 /
Client → Service → Pod 2
                 \
                  Pod 3
```

```bash
kubectl get svc
kubectl describe service payment-service
kubectl get endpoints
```

> **Interview crux:** Service gives a stable endpoint and routes traffic to Pods selected by labels.

---

## 8. Labels and Selectors

```bash
kubectl get pods --show-labels
kubectl describe service payment-service
```

Mental model:

```text
Service selector
      ↓
Pod label
```

If they do not match:

```text
Service has no endpoints
```

---

## 9. ConfigMap vs Secret

```text
Docker Image = application code
ConfigMap    = normal configuration
Secret       = sensitive configuration
```

```bash
kubectl get configmap
kubectl get secrets
```

> **Interview crux:** Build one immutable image and inject environment-specific configuration at runtime.

---

## 10. Liveness vs Readiness

```text
Liveness  = Is the application alive?
Readiness = Can this Pod receive traffic now?
```

> **Memory line:** Liveness decides restart. Readiness decides traffic.

---

## 11. Troubleshooting Flow

Always debug in this order:

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

Then check:

```text
/actuator/health
```

> **Memory:** GET → DESCRIBE → LOGS → SERVICE → HEALTH

---

## 12. Important Failure Scenarios

### CrashLoopBackOff

```text
Container starts
   ↓
Application crashes
   ↓
Kubernetes restarts it
```

Check:

```bash
kubectl logs <pod> --previous
kubectl describe pod <pod>
```

Typical causes:

```text
Bad configuration
Application exception
Database unavailable
Kafka unavailable
Probe failure
```

### ImagePullBackOff

> Kubernetes cannot download the image.

Check:

```bash
kubectl describe pod <pod>
```

Typical causes:

```text
Wrong image/tag
Private registry authentication missing
Registry unavailable
```

### Pending

> Kubernetes cannot schedule the Pod.

Typical causes:

```text
Not enough CPU or memory
Volume problem
Scheduling rule mismatch
```

### Pod Running but API Fails

Check:

```text
Pod → Application → Service → Endpoints
```

> Running Pod does not automatically mean reachable application.

### Service Has No Endpoints

Most common reason:

```text
Service selector ≠ Pod label
```

### More Pods but No More Throughput

```text
Pods scale
   ↓
Shared dependency does not
   ↓
Database / Kafka / Connection Pool becomes bottleneck
```

> **Architect answer:** Horizontal scaling only works when downstream dependencies also have enough capacity.

---

## 13. Rolling Deployment

```bash
kubectl rollout status deployment/payment-service
kubectl rollout history deployment/payment-service
kubectl rollout undo deployment/payment-service
```

> **Interview answer:** Deployment supports rolling updates so Pods can be replaced gradually, with rollback if the new version is unhealthy.

---

## 14. Jenkins → Docker → Kubernetes

```text
Git
 ↓
Jenkins
 ↓
Maven Build
 ↓
Docker Image
 ↓
Kubernetes Deployment
 ↓
Pod
 ↓
Service
 ↓
Payment API
```

> **Interview answer:** Jenkins automates CI/CD, Docker creates the deployable artifact, and Kubernetes manages that artifact in the runtime environment.

---

## 15. Prometheus + Grafana

```text
Payment App
    ↓ metrics
Prometheus
    ↓
Grafana
```

> Prometheus collects and stores metrics. Grafana visualizes them.

---

## 16. Kafka in the Same Architecture

```text
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

> **Interview crux:** Kafka is useful for asynchronous, event-driven communication between loosely coupled services.

---

## 17. Best 10 Commands to Remember

```bash
docker ps
docker logs <container>

kubectl get pods
kubectl get pods -o wide
kubectl describe pod <pod>
kubectl logs <pod>
kubectl logs <pod> --previous
kubectl get svc
kubectl get endpoints
kubectl rollout status deployment/<deployment>
```

These cover most interview troubleshooting discussions.

---

## 18. Five Interview Answers to Master

### Docker vs Kubernetes

> Docker packages and runs containers. Kubernetes manages containers at scale, including scheduling, self-healing, scaling, networking, and rolling deployment.

### What is a Pod?

> A Pod is Kubernetes' smallest deployable unit. It runs one or more closely related containers that share networking and lifecycle.

### What is a Deployment?

> A Deployment declares the desired number and version of Pods and manages rolling updates, rollback, and replacement of failed Pods.

### What is a Service?

> A Service gives stable network access to changing Pods and normally discovers them using label selectors.

### How do you troubleshoot a failing Pod?

> I check Pod status, then events with `describe`, then current or previous logs, followed by configuration, probes, dependencies, and resource limits.

---

## 19. Architect-Level Answer

### Explain how you deploy a Spring Boot microservice.

> I build the Spring Boot JAR and package it as an immutable Docker image. Kubernetes Deployment runs the required number of Pods from that image. A Service provides stable access. ConfigMaps and Secrets provide runtime configuration. Readiness and liveness probes control traffic and recovery. Jenkins automates deployment, while Prometheus and Grafana provide monitoring.

---

## 20. Hands-On Learning Order

```text
Docker Image
   ↓
Container
   ↓
Pod
   ↓
Deployment
   ↓
Self-Healing
   ↓
Scaling
   ↓
Service
   ↓
Labels
   ↓
ConfigMap / Secret
   ↓
Probes
   ↓
Troubleshooting
   ↓
Rollout
   ↓
Kafka
   ↓
Prometheus / Grafana
   ↓
Jenkins CI/CD
```

---

## 21. Final Memory Map

```text
CODE → BUILD → IMAGE → CONTAINER → POD → DEPLOYMENT → SERVICE → USER
```

Supporting concepts:

```text
Configuration → ConfigMap + Secret
Health        → Liveness + Readiness
Debug         → GET → DESCRIBE → LOGS
Deploy        → ROLLOUT → VERIFY → ROLLBACK
Async         → PRODUCER → KAFKA → CONSUMER
Monitoring    → APP → PROMETHEUS → GRAFANA
CI/CD         → GIT → JENKINS → DOCKER → KUBERNETES
```

---

## 22. Practice Rule

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

If you can deliberately break the Payment App, diagnose it, fix it, and explain why it failed, you know Docker and Kubernetes well enough to discuss them confidently in interviews.
