# Guia Jenkins para pipelines CircleGuard

Esta guia explica que debes hacer en Jenkins para demostrar los pipelines de CI/CD solicitados por el profesor. No necesitas memorizar Jenkins: sigue este orden y toma pantallazos de cada parte.

## 1. Que es Jenkins en este proyecto

Jenkins es el servidor que ejecuta automaticamente las fases definidas en cada `Jenkinsfile` del repositorio. En CircleGuard hay un `Jenkinsfile` por microservicio:

- `services/circleguard-auth-service/Jenkinsfile`
- `services/circleguard-identity-service/Jenkinsfile`
- `services/circleguard-gateway-service/Jenkinsfile`
- `services/circleguard-dashboard-service/Jenkinsfile`
- `services/circleguard-file-service/Jenkinsfile`
- `services/circleguard-form-service/Jenkinsfile`
- `services/circleguard-promotion-service/Jenkinsfile`
- `services/circleguard-notification-service/Jenkinsfile`

Cada pipeline compila, prueba, construye imagen Docker, escanea seguridad, despliega en Kubernetes y genera release notes.

## 2. Prerrequisitos en la maquina de Jenkins

El agente donde corre Jenkins debe tener:

1. Java 21.
2. Gradle wrapper del proyecto, ya incluido como `gradlew`/`gradlew.bat`.
3. Docker funcionando.
4. `kubectl` conectado al cluster Kubernetes.
5. Acceso al repositorio GitHub.
6. Acceso a un registry Docker.
7. SonarQube disponible si se desea ejecutar analisis real.
8. Trivy instalado o disponible como herramienta del agente.
9. Permisos Kubernetes para crear namespaces, deployments y services.

Comandos de verificacion en el agente:

```bash
java -version
docker ps
kubectl get nodes
kubectl get namespaces
```

## 3. Credenciales que debes configurar en Jenkins

En Jenkins entra a:

```text
Manage Jenkins > Credentials > System > Global credentials > Add Credentials
```

Crear estas credenciales:

| ID | Tipo sugerido | Uso |
|---|---|---|
| `docker-registry-url` | Secret text o Username/Password segun tu registry | Usado por `REGISTRY = credentials('docker-registry-url')` en los Jenkinsfile |
| `SONAR_TOKEN` | Secret text | Token para enviar analisis a SonarQube |
| `kubeconfig` | Secret file, si Jenkins no corre en tu maquina local | Acceso al cluster Kubernetes |

Nota: si haces la demo local y no tienes registry remoto, puedes explicar que `docker-registry-url` representa Docker Hub, GitHub Container Registry o el registry privado del curso. El pipeline ya esta preparado; para ejecucion real debes poner una URL valida y hacer login/push.

## 4. Crear un job Pipeline para un microservicio

Ejemplo con Auth:

1. Entrar a Jenkins.
2. Clic en `New Item`.
3. Nombre: `circleguard-auth-service-pipeline`.
4. Tipo: `Pipeline`.
5. Clic en `OK`.
6. Ir a la seccion `Pipeline`.
7. En `Definition`, elegir `Pipeline script from SCM`.
8. En `SCM`, elegir `Git`.
9. En `Repository URL`, poner la URL del repositorio.
10. En `Branch Specifier`, usar:

```text
*/master
```

o para stage:

```text
*/develop
```

11. En `Script Path`, poner:

```text
services/circleguard-auth-service/Jenkinsfile
```

12. Guardar.
13. Clic en `Build Now`.

Para los otros servicios repites lo mismo cambiando solo el `Script Path`.

## 5. Jobs recomendados

| Job Jenkins | Script Path |
|---|---|
| `circleguard-auth-service-pipeline` | `services/circleguard-auth-service/Jenkinsfile` |
| `circleguard-identity-service-pipeline` | `services/circleguard-identity-service/Jenkinsfile` |
| `circleguard-gateway-service-pipeline` | `services/circleguard-gateway-service/Jenkinsfile` |
| `circleguard-dashboard-service-pipeline` | `services/circleguard-dashboard-service/Jenkinsfile` |
| `circleguard-file-service-pipeline` | `services/circleguard-file-service/Jenkinsfile` |
| `circleguard-form-service-pipeline` | `services/circleguard-form-service/Jenkinsfile` |
| `circleguard-promotion-service-pipeline` | `services/circleguard-promotion-service/Jenkinsfile` |
| `circleguard-notification-service-pipeline` | `services/circleguard-notification-service/Jenkinsfile` |

## 6. Que hace cada fase del Jenkinsfile

| Fase | Explicacion |
|---|---|
| `Checkout` | Descarga el codigo desde Git. |
| `Compile and Package` | Compila el microservicio con Gradle. |
| `Unit Tests` | Ejecuta pruebas unitarias. |
| `Integration Tests` | Ejecuta pruebas `*IntegrationTest`. |
| `E2E Tests` | Ejecuta pruebas `*E2ETest`. |
| `Quality Gate - Full Service Test` | Ejecuta toda la suite del servicio. |
| `Static Analysis - SonarQube` | Envia codigo a SonarQube si `sonar-scanner` esta instalado. |
| `Docker Build and Push` | Construye la imagen Docker y la publica en registry. |
| `Container Vulnerability Scan - Trivy` | Escanea la imagen contra vulnerabilidades. |
| `Deploy Stage Kubernetes` | Despliega en `circleguard-stage` para ramas no master/main. |
| `System Validation Stage` | Verifica pods, services y rollout. |
| `Security Test - OWASP ZAP Baseline` | Ejecuta ZAP contra el endpoint stage. |
| `Locust Performance Smoke` | Ejecuta prueba corta de rendimiento. |
| `Semantic Version` | Calcula version basada en tags/build. |
| `Generate Release Notes` | Genera release notes automaticas. |
| `Production Approval` | Pide aprobacion manual antes de produccion. |
| `Deploy Production Kubernetes` | Despliega en ambiente productivo/master. |

## 7. Como ejecutar stage

Stage se usa para validar antes de produccion.

1. Crear o usar una rama distinta de `master/main`, por ejemplo `develop`.
2. Subir cambios:

```bash
git push origin develop
```

3. En Jenkins, ejecutar el job configurado con branch `*/develop`.
4. Confirmar fases verdes.
5. Validar Kubernetes:

```bash
kubectl -n circleguard-stage get pods
kubectl -n circleguard-stage get deployments
kubectl -n circleguard-stage get svc
kubectl -n circleguard-stage rollout status deployment/circleguard-auth-service
```

## 8. Como ejecutar master/produccion

1. Crear Pull Request hacia `master` o `main`.
2. Revisar y aprobar cambios.
3. Hacer merge.
4. Crear tag semantico:

```bash
git tag v1.0.0
git push origin master --tags
```

5. En Jenkins, ejecutar el job configurado con branch `*/master`.
6. Cuando aparezca `Production Approval`, aprobar manualmente.
7. Validar produccion:

```bash
kubectl -n circleguard-master get pods
kubectl -n circleguard-master get deployments
kubectl -n circleguard-master rollout status deployment/circleguard-auth-service
```

## 9. Release Notes y rollback

Las release notes se generan en:

```text
build/release-notes/<service>-<image-tag>.md
```

Deben incluir:

- Commits incluidos.
- Pruebas ejecutadas.
- Imagen Docker desplegada.
- Ambiente afectado.
- Comando de rollback.

Rollback:

```bash
kubectl rollout undo deployment/<microservicio> -n circleguard-master
```

Ejemplo:

```bash
kubectl rollout undo deployment/circleguard-auth-service -n circleguard-master
```

## 10. Pantallazos que debes tomar

| Pantallazo | Donde tomarlo |
|---|---|
| Configuracion del job | Jenkins > Job > Configure, mostrando Git, branch y Script Path |
| Jenkinsfile | Repositorio abierto en `services/.../Jenkinsfile` |
| Credencial Docker | Jenkins > Credentials, mostrando que existe `docker-registry-url` sin revelar secreto |
| Stage View | Jenkins > Job > Build, mostrando fases verdes |
| Log de pruebas | Consola Jenkins, fases Unit/Integration/E2E |
| Docker build/push | Consola Jenkins, fase Docker Build and Push |
| Trivy/Sonar/ZAP | Consola Jenkins, fases de seguridad y calidad |
| Kubernetes stage | Terminal con `kubectl -n circleguard-stage get pods` |
| Release Notes | Archivo generado en `build/release-notes/` |
| Aprobacion produccion | Pantalla de `Production Approval` |

## 11. Que decir en la sustentacion

> Jenkins automatiza el proceso de entrega. Cada microservicio tiene su propio Jenkinsfile para compilar, probar, analizar calidad, construir imagen Docker, escanear vulnerabilidades y desplegar en Kubernetes. Para stage el despliegue es automatico; para master/produccion se agrega aprobacion manual y release notes, cumpliendo Change Management.

## 12. Problemas comunes

| Problema | Causa | Solucion |
|---|---|---|
| `docker: command not found` | Docker no esta instalado en el agente | Instalar Docker o usar agente con Docker |
| `kubectl connection refused` | Kubernetes no esta activo o kubeconfig incorrecto | Verificar `kubectl get nodes` |
| `credentials not found` | No existe `docker-registry-url` | Crear credencial con ese ID exacto |
| Falla `docker push` | Registry invalido o sin login | Configurar registry real y credenciales |
| Sonar no corre | No existe `sonar-scanner` | Instalar scanner o plugin Jenkins |
| Trivy no corre | Trivy no instalado | Instalar CLI Trivy en el agente |

