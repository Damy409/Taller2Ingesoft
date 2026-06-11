# Guia de sustentacion del proyecto final

Esta guia explica que se implemento, donde esta cada evidencia y como mostrarlo en la sustentacion. El objetivo es que puedas defender el proyecto en el mismo orden de los requisitos del profesor.

## 0. Preparacion antes de grabar o sustentar

1. Abrir Docker Desktop.
2. En Docker Desktop, confirmar que Kubernetes este habilitado.
3. Esperar a que Docker muestre estado `Running`.
4. Abrir PowerShell o Git Bash en:

```powershell
cd C:\Users\Damy\Documents\ingesoft\circle-guard-public
```

5. Verificar herramientas:

```powershell
docker ps
kubectl get nodes
.\gradlew.bat test
```

Nota importante: el proyecto fuerza JDK 21 desde `gradlew.bat` si existe en `C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot`. Esto evita el error de Java 25 (`IllegalArgumentException: 25.0.3`) durante la demo.

## 1. Metodologia agil y branching

Archivo de evidencia: `docs/agile/AGILE_AND_BRANCHING.md`.

Que se hizo:

- Se definio Scrum ligero con tablero Kanban.
- Se documentaron dos iteraciones completas.
- Se crearon historias de usuario con criterios de aceptacion.
- Se definio una estrategia tipo GitHub Flow/GitFlow simplificado.
- Se explico como se promocionan cambios desde ramas de desarrollo hasta `master`.

Que decir:

> Para el proyecto se documento una metodologia agil con dos iteraciones. Cada historia tiene criterios de aceptacion y se conecta con pruebas, pipeline y despliegue. La estrategia de branching separa cambios de feature, integracion en stage y despliegue controlado a master/prod.

## 2. Arquitectura de microservicios

Archivo de evidencia: `docs/architecture/ARCHITECTURE.md`.

Que se hizo:

- Se documentaron los microservicios Auth, Identity, Gateway, Dashboard, File, Form, Promotion y Notification.
- Se describio la comunicacion entre servicios, Kafka, PostgreSQL, Neo4j y Kubernetes.
- Se incluyeron diagramas Mermaid para explicar la arquitectura.

Que decir:

> La arquitectura esta separada por responsabilidades. Auth maneja autenticacion, Identity anonimiza identidades, Gateway centraliza validaciones, y los servicios de negocio se comunican mediante HTTP y eventos Kafka. Kubernetes permite desplegar cada servicio de forma independiente.

## 3. Infraestructura como Codigo con Terraform

Archivos de evidencia:

- `infra/terraform/README.md`
- `infra/terraform/modules/namespace/main.tf`
- `infra/terraform/modules/microservice/main.tf`
- `infra/terraform/modules/observability/main.tf`
- `infra/terraform/envs/dev/main.tf`
- `infra/terraform/envs/stage/main.tf`
- `infra/terraform/envs/prod/main.tf`

Que se hizo:

- Se creo una estructura modular.
- Se definieron ambientes `dev`, `stage` y `prod`.
- Se documento backend remoto con S3 y DynamoDB para estado y bloqueo.
- Se modelaron namespaces, despliegues de microservicios y observabilidad.

Comandos para mostrar si tienes Terraform instalado:

```powershell
cd infra\terraform\envs\dev
terraform init
terraform plan -var="registry=local"
```

Que decir:

> Terraform permite reproducir la infraestructura por ambiente. Los modulos evitan repetir manifiestos y el backend remoto protege el estado compartido del equipo.

## 4. Patrones de diseno implementados

Archivos de evidencia:

- `docs/architecture/DESIGN_PATTERNS.md`
- `services/circleguard-auth-service/src/main/java/com/circleguard/auth/resilience/SimpleCircuitBreaker.java`
- `services/circleguard-auth-service/src/main/java/com/circleguard/auth/client/IdentityClient.java`

Que se hizo:

- Se identificaron patrones existentes de capas, DTO, repository, controller-service y event-driven.
- Se agrego Circuit Breaker simple para proteger la comunicacion Auth -> Identity.
- Se agrego External Configuration usando propiedades Spring.
- Se agrego Feature Toggle con `circleguard.features.identity-fallback`.

Que decir:

> El Circuit Breaker evita que una falla temporal de Identity bloquee Auth. Si Identity no responde, se usa un fallback deterministico configurable. Esto mejora resiliencia sin cambiar el contrato del servicio.

## 5. CI/CD avanzado

Archivos de evidencia:

- `.github/workflows/advanced-ci.yml`
- `services/<microservicio>/Jenkinsfile`
- `CI_CD_EVIDENCE_GUIDE.md`

Que se hizo:

- Cada microservicio tiene Jenkinsfile.
- Se agrego workflow de GitHub Actions como alternativa CI.
- Los pipelines incluyen build, pruebas unitarias, integracion, E2E, analisis SonarQube, Docker, Trivy, despliegue a stage, validacion del sistema, ZAP, Locust, versionado, release notes, aprobacion manual y despliegue a produccion.
- Se documenta rollback con `kubectl rollout undo`.

Que decir:

> El pipeline de stage despliega automaticamente ramas de desarrollo en `circleguard-stage`. El pipeline de master incluye aprobacion manual, genera release notes y despliega en ambiente productivo controlado.

## 6. Pruebas completas

Archivos de evidencia:

- `docs/testing/TEST_ANALYSIS.md`
- `tests/locust_tests.py`
- `tests/performance_config.py`
- `CI_CD_EVIDENCE_GUIDE.md`

Comandos ejecutados y verificados:

```powershell
.\gradlew.bat test
.\gradlew.bat build
python -m py_compile tests\locust_tests.py tests\performance_config.py
```

Resultado validado el 2026-06-10:

- `test`: BUILD SUCCESSFUL.
- `build`: BUILD SUCCESSFUL.
- Total JUnit: 112 pruebas, 0 fallos, 0 errores, 7 omitidas por depender de Docker/Testcontainers.
- JAR Auth generado: `services/circleguard-auth-service/build/libs/circleguard-auth-service-1.0.0-SNAPSHOT.jar`.

Que decir:

> Las pruebas cubren componentes individuales, comunicacion entre servicios, flujos completos y rendimiento con Locust. Las pruebas omitidas son intencionales cuando Docker no esta activo, porque dependen de Testcontainers.

## 7. Orden recomendado para el video corto

Ejecutar pruebas:

```powershell
.\gradlew.bat test
```

Construir proyecto:

```powershell
.\gradlew.bat build
```

Verificar JAR:

```powershell
dir services\circleguard-auth-service\build\libs
```

Verificar Docker:

```powershell
docker ps
docker build -t circleguard-auth-service .
docker images circleguard-auth-service
```

Construir imagen desde el microservicio:

```powershell
cd services\circleguard-auth-service
.\gradlew.bat build
dir build\libs
docker build -t circleguard-auth-service .
```

Crear namespaces:

```powershell
kubectl create namespace circleguard-stage
kubectl create namespace circleguard-master
kubectl get namespaces
```

Verificar Kubernetes:

```powershell
kubectl get nodes
```

Desplegar Auth con PostgreSQL:

```powershell
kubectl apply -f k8s\
kubectl get pods -A
kubectl get deployments -A
kubectl get svc -A
```

Ver logs:

```powershell
kubectl get pods -A
kubectl logs <NOMBRE_DEL_POD> -n circleguard-stage --tail=80
```

Recrear despliegue si necesitas mostrar recuperacion:

```powershell
kubectl delete deployment circleguard-auth-service -n circleguard-stage
kubectl apply -f k8s\
kubectl rollout status deployment/circleguard-auth-service -n circleguard-stage
```

Crear release notes:

```powershell
cd C:\Users\Damy\Documents\ingesoft\circle-guard-public
mkdir build\release-notes -Force
git log --pretty=format:"- %h %s (%an)" > build\release-notes\release.md
notepad build\release-notes\release.md
```

## 8. Despliegue master environment paso a paso

1. Hacer merge de la rama validada hacia `master`.
2. Crear tag semantico, por ejemplo:

```powershell
git tag v1.0.0
git push origin master --tags
```

3. Ejecutar el Jenkinsfile del microservicio desde la rama `master`.
4. Revisar fases de pipeline:

- Checkout.
- Compile and Package.
- Unit Tests.
- Integration Tests.
- E2E Tests.
- Quality Gate.
- SonarQube.
- Docker Build and Push.
- Trivy.
- Generate Release Notes.
- Production Approval.
- Deploy Production Kubernetes.

5. Aprobar manualmente el stage `Production Approval`.
6. Verificar produccion:

```powershell
kubectl get pods -n circleguard-master
kubectl get svc -n circleguard-master
kubectl rollout status deployment/circleguard-auth-service -n circleguard-master
```

7. Si falla, aplicar rollback:

```powershell
kubectl rollout undo deployment/circleguard-auth-service -n circleguard-master
```

## 9. Observabilidad y monitoreo

Archivos de evidencia:

- `k8s/observability/prometheus.yaml`
- `k8s/observability/grafana.yaml`
- `k8s/observability/elk-stack.yaml`
- `k8s/observability/jaeger.yaml`

Comando:

```powershell
kubectl apply -f k8s\observability\
kubectl -n observability get pods
```

Que decir:

> Prometheus recolecta metricas tecnicas, Grafana permite dashboards, ELK centraliza logs y Jaeger permite tracing distribuido. Los servicios tambien tienen probes de salud en Kubernetes.

## 10. Seguridad

Archivos de evidencia:

- `k8s/security/rbac-stage.yaml`
- `k8s/security/network-policy.yaml`
- `k8s/security/secrets-example.yaml`
- `k8s/security/tls-ingress.yaml`
- `security/zap-baseline.yaml`

Que se hizo:

- RBAC para limitar permisos.
- Secrets como base para no quemar credenciales.
- TLS para servicios expuestos.
- NetworkPolicy para aislar trafico.
- Trivy en pipeline para imagenes.
- OWASP ZAP baseline para pruebas de seguridad dinamicas.

## 11. Entregables finales

Entregar:

- Repositorio completo.
- `README.md` actualizado.
- `CI_CD_EVIDENCE_GUIDE.md` como guia principal de evidencia.
- Carpeta `docs/` con metodologia, arquitectura, patrones, pruebas, operaciones, costos y sustentacion.
- Carpeta `infra/terraform/`.
- Carpeta `k8s/`.
- Carpeta `security/`.
- Zip final generado desde el proyecto.

## 12. Lecciones aprendidas

- Separar configuracion por ambiente evita recompilar imagenes.
- Kubernetes requiere dependencias internas bien nombradas, como PostgreSQL para Auth.
- Pruebas automatizadas evitan regresiones antes del despliegue.
- Observabilidad, rollback y release notes hacen parte de operar el sistema, no solo de desplegarlo.
## 13. Evidencia actualizada con Docker y Kubernetes activos

Validacion ejecutada el 2026-06-11:

```powershell
.\gradlew.bat cleanTest test
.\gradlew.bat build
docker build -t circleguard-auth-service .
kubectl apply -n circleguard-stage -f services\circleguard-auth-service\k8s\
kubectl -n circleguard-stage rollout status deployment/circleguard-auth-service
kubectl -n circleguard-stage logs <pod-auth> --tail=80
```

Resultados para mostrar en la sustentacion:

- Docker Desktop activo.
- Kubernetes `docker-desktop` en estado `Ready`, version `v1.34.1`.
- Tests Gradle: `BUILD SUCCESSFUL`, 112 pruebas, 0 fallos, 0 errores.
- Build Gradle: `BUILD SUCCESSFUL`.
- Imagen Docker: `circleguard-auth-service:latest`, ID `186b1e397acb`.
- Namespace `circleguard-stage`: activo.
- Pods stage: Auth y PostgreSQL `1/1 Running`.
- Deployments stage: Auth y PostgreSQL `1/1 Available`.
- Logs Auth: Spring Boot inicia con Java 21, conecta a PostgreSQL, aplica 5 migraciones Flyway y levanta Tomcat en puerto 8180.

Archivo complementario para Jenkins: `docs/ci-cd/JENKINS_PIPELINE_GUIDE.md`.
Archivo de revision de completitud: `docs/presentation/PROJECT_COMPLETION_REVIEW.md`.


