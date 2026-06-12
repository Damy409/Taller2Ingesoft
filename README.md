# CircleGuard - Proyecto Final IngeSoft V

Damy Villegas - A00398942

CircleGuard es una arquitectura de microservicios para trazabilidad, control de acceso y contencion sanitaria en campus universitario. El proyecto integra practicas modernas de DevOps, seguridad, pruebas, observabilidad e infraestructura como codigo.

## Cobertura de requisitos

- Metodologia agil y branching documentados.
- Terraform modular multiambiente.
- Patrones de diseno, resiliencia, configuracion y feature toggles.
- CI/CD avanzado con Jenkins/GitHub Actions, SonarQube, Trivy y OWASP ZAP.
- Pruebas unitarias, integracion, E2E, rendimiento y seguridad.
- Change Management, release notes, tags y rollback.
- Prometheus, Grafana, ELK y Jaeger.
- Seguridad con RBAC, secretos, TLS y NetworkPolicy.
- Manual de operaciones, costos y guia de sustentacion.

## Microservicios

| Servicio | Puerto | Responsabilidad |
|---|---:|---|
| Auth | 8180 | Autenticacion, JWT, RBAC, handoff visitantes |
| Identity | 8083 | Vault de anonimizacion |
| Gateway | 8087 | Validacion de entrada y QR |
| Promotion | 8088 | Propagacion de estados en grafo Neo4j |
| Notification | 8082 | Email, SMS, push y listeners Kafka |
| Form | 8086 | Cuestionarios dinamicos y eventos |
| File | 8085 | Certificados y documentos |
| Dashboard | 8084 | Analiticas y privacidad k-anonimato |


## Ejecucion local

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

## Docker demo Auth

```powershell
docker build -t circleguard-auth-service .
docker images circleguard-auth-service
```

## Kubernetes demo Auth

```powershell
cd services\circleguard-auth-service
kubectl apply -f k8s\
kubectl get pods -A
kubectl logs <pod-auth> --tail=80
```

## Terraform

```bash
cd infra/terraform/envs/dev
terraform init
terraform plan -var='registry=<registry-url>'
```

## Observabilidad

```powershell
kubectl apply -f k8s\observability\
kubectl -n observability get pods
```

## Seguridad

- RBAC: `k8s/security/rbac-stage.yaml`
- NetworkPolicy: `k8s/security/network-policy.yaml`
- TLS ingress: `k8s/security/tls-ingress.yaml`
- Secret example: `k8s/security/secrets-example.yaml`
- ZAP baseline: `security/zap-baseline.yaml`

## Release notes

```powershell
mkdir build\release-notes -Force
git log --pretty=format:"- %h %s (%an)" > build\release-notes\release.md
```
## Rollback

```powershell
kubectl rollout undo deployment/<microservicio> -n circleguard-master
```




