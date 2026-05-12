# Guia unica de evidencia: pruebas, pipelines, despliegue y entrega

Fecha de actualizacion: 2026-05-11

Este documento consolida toda la evidencia requerida para la entrega. Reemplaza a
`TEST_SUMMARY.md`, `TESTING_ANALYSIS.md`, `TESTING_GUIDE.md` y
`TESTS_QUICK_REFERENCE.md`.

## 1. Alcance

Microservicios incluidos en la validacion:

| Microservicio | Pipeline | Dockerfile | Kubernetes |
|---|---|---|---|
| circleguard-auth-service | `services/circleguard-auth-service/Jenkinsfile` | Si | Si |
| circleguard-identity-service | `services/circleguard-identity-service/Jenkinsfile` | Si | Si |
| circleguard-gateway-service | `services/circleguard-gateway-service/Jenkinsfile` | Si | Si |
| circleguard-dashboard-service | `services/circleguard-dashboard-service/Jenkinsfile` | Si | Si |
| circleguard-file-service | `services/circleguard-file-service/Jenkinsfile` | Si | Si |
| circleguard-form-service | `services/circleguard-form-service/Jenkinsfile` | Si | Si |
| circleguard-promotion-service | `services/circleguard-promotion-service/Jenkinsfile` | Si | Si |
| circleguard-notification-service | `services/circleguard-notification-service/Jenkinsfile` | Si | Si |

## 2. Pruebas implementadas

La suite cubre las cuatro categorias solicitadas:

| Categoria | Evidencia | Cantidad |
|---|---|---:|
| Unitarias | `*ServiceTest`, `*ControllerTest`, `*RepositoryTest`, `*ConverterTest`, `*ListenerTest` | 25 archivos |
| Integracion | `*IntegrationTest` | 5 archivos |
| E2E | `*E2ETest` | 5 archivos |
| Rendimiento | `tests/locust_tests.py`, `tests/performance_config.py`, `PromotionPerformanceTest` | 2 scripts + 1 test |

Funciones principales validadas:

- Auth: login, JWT, permisos y comunicacion con identity.
- Identity: anonimato, resolucion de identidad, endpoint de visitante y lookup auditado.
- Gateway: validacion de QR y flujo de acceso.
- Dashboard: consulta de analiticas, rangos, metricas y agregacion.
- File: autenticacion, upload, listado, descarga y rechazo de archivos invalidos.
- Form: envio de cuestionarios, validacion, persistencia y evento Kafka.
- Notification: plantillas, despacho, listeners, reintentos y canales.
- Promotion: reglas de estado de salud, permisos administrativos y propagacion Neo4j cuando Docker esta disponible.

## 3. Resultados ejecutados localmente

Comando ejecutado:

```bash
.\gradlew.bat test
```

Resultado final:

```text
BUILD SUCCESSFUL
46 actionable tasks: 9 executed, 37 up-to-date
```

Resumen JUnit actualizado desde `services/**/build/test-results/test/TEST-*.xml`:

| Microservicio | Tests | Fallos | Errores | Omitidas | Tiempo |
|---|---:|---:|---:|---:|---:|
| circleguard-auth-service | 9 | 0 | 0 | 0 | 0.46 s |
| circleguard-dashboard-service | 15 | 0 | 0 | 0 | 0.96 s |
| circleguard-file-service | 16 | 0 | 0 | 0 | 0.74 s |
| circleguard-form-service | 15 | 0 | 0 | 0 | 3.04 s |
| circleguard-gateway-service | 3 | 0 | 0 | 0 | 1.17 s |
| circleguard-identity-service | 23 | 0 | 0 | 0 | 1.98 s |
| circleguard-notification-service | 11 | 0 | 0 | 0 | 5.10 s |
| circleguard-promotion-service | 20 | 0 | 0 | 7 | 1.07 s |
| Total | 112 | 0 | 0 | 7 | 14.52 s |

Las 7 pruebas omitidas pertenecen a escenarios con Testcontainers/Docker. Quedan configuradas con `@Testcontainers(disabledWithoutDocker = true)` para ejecutarse automaticamente cuando el agente de CI tenga Docker disponible.

Validaciones adicionales ejecutadas:

```bash
python -m py_compile tests\locust_tests.py tests\performance_config.py
```

Resultado: sin errores de sintaxis.

```bash
kubectl apply --dry-run=client --validate=false -f services\<service>\k8s\*.yaml
```

Resultado: todos los manifiestos Kubernetes parsean correctamente con `kubectl` en modo client dry-run.

## 4. Configuracion de pipelines

Todos los pipelines tienen la misma estructura. Cambian las variables `SERVICE_NAME`, `SERVICE_PATH`, `GRADLE_PROJECT` y `SERVICE_PORT`.

Variables por pipeline:

| Pipeline | SERVICE_NAME | GRADLE_PROJECT | SERVICE_PORT |
|---|---|---|---:|
| Auth | `circleguard-auth-service` | `:services:circleguard-auth-service` | 8180 |
| Identity | `circleguard-identity-service` | `:services:circleguard-identity-service` | 8083 |
| Gateway | `circleguard-gateway-service` | `:services:circleguard-gateway-service` | 8087 |
| Dashboard | `circleguard-dashboard-service` | `:services:circleguard-dashboard-service` | 8084 |
| File | `circleguard-file-service` | `:services:circleguard-file-service` | 8085 |
| Form | `circleguard-form-service` | `:services:circleguard-form-service` | 8086 |
| Promotion | `circleguard-promotion-service` | `:services:circleguard-promotion-service` | 8088 |
| Notification | `circleguard-notification-service` | `:services:circleguard-notification-service` | 8082 |

Fases definidas:

1. `Checkout`: obtiene codigo fuente.
2. `Compile and Package`: compila y empaqueta el microservicio.
3. `Unit Tests`: ejecuta pruebas unitarias enfocadas.
4. `Integration Tests`: ejecuta `*IntegrationTest`.
5. `E2E Tests`: ejecuta `*E2ETest`.
6. `Quality Gate - Full Service Test`: ejecuta toda la suite del microservicio.
7. `Docker Build and Push`: construye imagen desde el monorepo y la publica.
8. `Deploy Stage Kubernetes`: despliega al namespace `circleguard-stage`.
9. `System Validation Stage`: valida pods, services y rollout.
10. `Locust Performance Smoke`: ejecuta prueba corta de rendimiento contra stage.
11. `Generate Release Notes`: en `master/main`, genera release notes automaticas.
12. `Deploy Master Kubernetes`: en `master/main`, despliega al namespace `circleguard-master`.

Fragmento clave comun del pipeline:

```groovy
stage('Unit Tests') {
    steps {
        sh '''
            set -e
            if find "$SERVICE_PATH/src/test/java" -name "*ServiceTest.java" -o -name "*ControllerTest.java" -o -name "*RepositoryTest.java" -o -name "*ConverterTest.java" | grep -q .; then
              ./gradlew "$GRADLE_PROJECT:test" --tests "*ServiceTest" --tests "*ControllerTest" --tests "*RepositoryTest" --tests "*ConverterTest"
            else
              echo "No focused unit test classes for $SERVICE_NAME; covered by full test stage."
            fi
        '''
    }
}

stage('Generate Release Notes') {
    when { anyOf { branch 'master'; branch 'main' } }
    steps {
        sh '''
            mkdir -p build/release-notes
            LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || true)
            if [ -n "$LAST_TAG" ]; then RANGE="$LAST_TAG..HEAD"; else RANGE="HEAD~20..HEAD"; fi
            NOTES="build/release-notes/$SERVICE_NAME-$IMAGE_TAG.md"
            {
              echo "# Release Notes - $SERVICE_NAME $IMAGE_TAG"
              echo "## Change Summary"
              git log --pretty=format:'- %h %s (%an)' $RANGE -- $SERVICE_PATH || true
              echo "## Validation Evidence"
              echo "- Unit, integration and E2E tests executed by Jenkins."
              echo "- Kubernetes rollout validated in stage before master deployment."
              echo "- Locust smoke report archived when performance tests are available."
              echo "## Change Management"
              echo "- Deployment image: $REGISTRY/$SERVICE_NAME:$IMAGE_TAG"
              echo "- Rollback: kubectl rollout undo deployment/$SERVICE_NAME -n $MASTER_NAMESPACE"
            } > "$NOTES"
        '''
    }
}
```

Pantallazos requeridos para esta seccion:

- Jenkins job configuration de cada microservicio.
- Credencial `docker-registry-url` configurada en Jenkins.
- Jenkinsfile abierto en el repositorio, mostrando las fases.
- Configuracion de acceso al cluster Kubernetes desde el agente Jenkins.

## 5. Guia paso a paso para desplegar con Jenkins y Kubernetes

### 5.1 Prerrequisitos

En la maquina/agente de Jenkins debes tener:

1. Java 21.
2. Docker funcionando.
3. `kubectl` conectado al cluster Kubernetes.
4. Acceso al repositorio Git.
5. Credencial de registry Docker en Jenkins con id exacto `docker-registry-url`.
6. Permisos para crear namespaces y deployments en Kubernetes.

Verificacion rapida:

```bash
java -version
docker --version
kubectl version --client
kubectl get nodes
```

### 5.2 Crear namespaces

```bash
kubectl create namespace circleguard-stage --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace circleguard-master --dry-run=client -o yaml | kubectl apply -f -
```

### 5.3 Configurar Jenkins

Para cada microservicio:

1. Crear un job tipo `Pipeline`.
2. En `Pipeline`, seleccionar `Pipeline script from SCM`.
3. SCM: Git.
4. Repository URL: URL del repositorio.
5. Branch stage: la rama de desarrollo o entrega, por ejemplo `*/develop`.
6. Branch master: `*/master` o `*/main`.
7. Script Path: usar el Jenkinsfile del microservicio. Ejemplo:

```text
services/circleguard-auth-service/Jenkinsfile
```

8. Repetir para cada microservicio con su Script Path correspondiente.

### 5.4 Ejecutar pipeline de stage

1. Hacer push a una rama distinta a `master/main`.
2. Ejecutar el job en Jenkins.
3. Validar que pasen estas fases:
   - Compile and Package.
   - Unit Tests.
   - Integration Tests.
   - E2E Tests.
   - Quality Gate - Full Service Test.
   - Docker Build and Push.
   - Deploy Stage Kubernetes.
   - System Validation Stage.
   - Locust Performance Smoke.
4. Revisar pods:

```bash
kubectl -n circleguard-stage get pods
kubectl -n circleguard-stage get svc
kubectl -n circleguard-stage rollout status deployment/circleguard-auth-service
```

### 5.5 Ejecutar pipeline de master

1. Crear Pull Request hacia `master` o `main`.
2. Revisar cambios y aprobar.
3. Hacer merge.
4. Ejecutar el job de Jenkins sobre `master/main`.
5. Confirmar estas fases:
   - Build.
   - Unit Tests.
   - Integration Tests.
   - E2E Tests.
   - Quality Gate.
   - Docker Build and Push.
   - Locust Performance Smoke.
   - Generate Release Notes.
   - Deploy Master Kubernetes.
6. Revisar deployment:

```bash
kubectl -n circleguard-master get pods
kubectl -n circleguard-master get svc
kubectl -n circleguard-master rollout status deployment/circleguard-auth-service
```

### 5.6 Release Notes y rollback

El pipeline genera release notes en:

```text
build/release-notes/<service>-<image-tag>.md
```

Contenido esperado:

- Resumen de commits.
- Evidencia de pruebas ejecutadas.
- Imagen desplegada.
- Comando de rollback.

Rollback:

```bash
kubectl rollout undo deployment/<microservicio> -n circleguard-master
```

Ejemplo:

```bash
kubectl rollout undo deployment/circleguard-auth-service -n circleguard-master
```

## 6. Resultado esperado de pipelines

Para cada pipeline se debe capturar:

| Evidencia | Que mostrar en el pantallazo |
|---|---|
| Configuracion | Pantalla de Jenkins con SCM, branch y Script Path |
| Etapas | Stage View o Blue Ocean con todas las fases en verde |
| Pruebas | Reporte JUnit publicado por Jenkins |
| Docker | Log de `docker build` y `docker push` exitoso |
| Kubernetes stage | Log de `rollout status` en `circleguard-stage` |
| Kubernetes master | Log de `rollout status` en `circleguard-master` |
| Release Notes | Archivo generado en `build/release-notes/` |
| Locust | Artefactos CSV en `build/locust/` |

## 7. Analisis de resultados

### 7.1 Pruebas unitarias

Las pruebas unitarias validan componentes aislados. El resultado local muestra 0 fallos y 0 errores. Esto indica que la logica de servicios, controladores, repositorios y convertidores es estable para los casos cubiertos.

Riesgos cubiertos:

- Generacion y validacion JWT.
- Conversion/encriptacion de identidad.
- Validacion de formularios.
- Validacion de archivos.
- Plantillas y despacho de notificaciones.

### 7.2 Pruebas de integracion

Las pruebas de integracion validan comunicacion entre servicios y dependencias simuladas:

- Auth con Identity.
- Identity con persistencia/eventos.
- Dashboard con base de datos.
- File con Auth.
- Form con Auth y Kafka.

Resultado: 5 archivos de integracion, todos dentro de la suite con 0 fallos.

### 7.3 Pruebas E2E

Los E2E validan flujos completos:

- Autenticacion completa.
- Gestion de identidad.
- Dashboard y metricas.
- Gestion de archivos.
- Envio de formularios.

Resultado: 5 archivos E2E, todos dentro de la suite con 0 fallos.

### 7.4 Rendimiento y estres con Locust

El script `tests/locust_tests.py` simula casos reales:

- Login y refresh token.
- Mapeo de identidad y visitantes.
- Consulta de analytics.
- Upload, listado y descarga de archivos.
- Envio y consulta de formularios.
- Health check y discovery del gateway.

Comando de carga base:

```bash
python -m locust -f tests/locust_tests.py --headless --host=http://<gateway-stage>:8087 -u 50 -r 5 -t 5m --csv=build/locust/load
```

Comando de estres:

```bash
python -m locust -f tests/locust_tests.py --headless --host=http://<gateway-stage>:8087 -u 200 -r 10 -t 10m --csv=build/locust/stress
```

Metricas clave a reportar desde los CSV de Locust:

| Metrica | Interpretacion | Umbral sugerido |
|---|---|---|
| Average response time | Promedio de latencia por endpoint | Menor a 1000 ms |
| 95% response time | Latencia percibida por la mayoria de usuarios | Menor a 2000 ms |
| Requests/s | Throughput sostenido | Debe crecer con usuarios hasta saturacion |
| Failures/s | Errores por segundo | Cercano a 0 en carga base |
| Failure rate | Porcentaje de requests fallidos | Menor a 5% |

Estado local: los scripts de Locust compilan correctamente. La prueba de carga real debe ejecutarse cuando el gateway este desplegado en `circleguard-stage`, porque sin endpoint activo no se pueden obtener latencia, throughput ni tasa de errores reales.

## 8. Guia para video maximo 8 minutos

Duracion sugerida:

1. 0:00 - 0:45: mostrar estructura del proyecto, `services/`, `tests/` y este documento.
2. 0:45 - 1:45: explicar pruebas unitarias, integracion y E2E con ejemplos de archivos.
3. 1:45 - 2:30: ejecutar o mostrar resultado de `.\gradlew.bat test` con `BUILD SUCCESSFUL`.
4. 2:30 - 3:15: abrir reporte HTML de JUnit, por ejemplo `services/circleguard-auth-service/build/reports/tests/test/index.html`.
5. 3:15 - 4:00: mostrar `tests/locust_tests.py` y explicar escenarios de rendimiento.
6. 4:00 - 5:15: abrir un Jenkinsfile y explicar fases stage/master.
7. 5:15 - 6:00: mostrar manifiestos `k8s/deployment.yaml` y `k8s/service.yaml`.
8. 6:00 - 7:00: explicar ejecucion del pipeline en Jenkins, Release Notes y rollback.
9. 7:00 - 8:00: mostrar el zip final y cerrar con resultados principales.

## 8.1 Guion verificado de comandos para el video

Estos comandos fueron revisados contra el proyecto actual. Ejecutarlos desde:

```powershell
cd C:\Users\Damy\Documents\ingesoft\circle-guard-public
```

### 1. Ejecutar tests

```powershell
.\gradlew.bat test
```

Resultado esperado: `BUILD SUCCESSFUL`.

### 2. Construir proyecto

```powershell
.\gradlew.bat build
```

Resultado esperado: `BUILD SUCCESSFUL`.

### 3. Verificar jars en build/libs

```powershell
dir services\circleguard-auth-service\build\libs
```

Debe aparecer:

- `circleguard-auth-service-1.0.0-SNAPSHOT.jar`
- `circleguard-auth-service-1.0.0-SNAPSHOT-plain.jar`

### 4. Verificar Docker

```powershell
docker ps
docker build -t circleguard-auth-service .
docker images circleguard-auth-service
```

El `Dockerfile` de la raiz construye la imagen del auth service usando el jar generado por Gradle.

### 5. Imagen Docker desde carpeta del microservicio

Importante: dentro de `services\circleguard-auth-service` no existe `gradlew.bat`. Por eso el build del servicio se ejecuta desde la raiz:

```powershell
.\gradlew.bat :services:circleguard-auth-service:build
cd services\circleguard-auth-service
dir build\libs
docker build -t circleguard-auth-service:service-context .
cd ..\..
```

### 6. Crear namespaces Kubernetes

Si los namespaces no existen:

```powershell
kubectl create namespace circleguard-stage
kubectl create namespace circleguard-master
```

Si ya existen, usa la version idempotente para que no falle durante el video:

```powershell
kubectl create namespace circleguard-stage --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace circleguard-master --dry-run=client -o yaml | kubectl apply -f -
kubectl get namespaces
docker images
```

### 7. Verificar Kubernetes

```powershell
kubectl get nodes
```

Resultado verificado localmente: nodo `docker-desktop` en estado `Ready`.

### 8. Mostrar archivos de deployment

```powershell
dir services\circleguard-auth-service\k8s
type services\circleguard-auth-service\k8s\deployment.yaml
type services\circleguard-auth-service\k8s\service.yaml
type services\circleguard-auth-service\k8s\postgres.yaml
```

`postgres.yaml` se incluye para que el auth service pueda arrancar en Kubernetes durante la demo.

### 9. Aplicar manifiestos y ver pods

```powershell
cd services\circleguard-auth-service
kubectl apply -f k8s\
kubectl get pods -A
```

Resultado esperado:

- `circleguard-auth-postgres` en `Running`.
- `circleguard-auth-service` en `Running`.

### 10. Ver deployments

```powershell
kubectl get deployments -A
```

Resultado esperado para auth:

- `circleguard-auth-postgres` con `1/1`.
- `circleguard-auth-service` con `1/1`.

### 11. Ver servicios

```powershell
kubectl get svc -A
```

Resultado esperado:

- `circleguard-auth-service` en puerto `8180/TCP`.
- `circleguard-auth-postgres` en puerto `5432/TCP`.

### 12. Ver logs y recreacion de pod

```powershell
kubectl get pods -A
$pod = kubectl get pods -l app=circleguard-auth-service -o jsonpath='{.items[0].metadata.name}'
kubectl logs $pod --tail=60
kubectl delete pod $pod
kubectl get pods -l app=circleguard-auth-service
```

En los logs debe verse:

- `Tomcat started on port 8180`.
- `Successfully applied 5 migrations`.
- `Started AuthServiceApplication`.

Al borrar el pod, Kubernetes debe crear uno nuevo automaticamente.

### 13. Crear Release Notes

```powershell
cd C:\Users\Damy\Documents\ingesoft\circle-guard-public
mkdir build\release-notes -Force
git log --pretty=format:"- %h %s (%an)" > build\release-notes\release.md
type build\release-notes\release.md
```

Esto evidencia el resumen automatico de cambios usado por el pipeline para Change Management.

## 9. Zip de entrega

Crear el zip final desde la raiz del proyecto:

```powershell
$zip = "circle-guard-entrega-pruebas-pipelines.zip"
if (Test-Path $zip) { Remove-Item $zip }
Get-ChildItem -Force |
  Where-Object { $_.Name -notin @(".git", ".gradle", "build", $zip) } |
  Compress-Archive -DestinationPath $zip
```

El zip debe incluir:

- Proyecto modificado.
- Pipelines `Jenkinsfile`.
- Pruebas implementadas.
- Scripts Locust.
- Manifiestos Kubernetes.
- Esta guia unica de evidencia.

## 10. Checklist final

- Minimo cinco pruebas unitarias.
- Minimo cinco pruebas de integracion.
- Minimo cinco pruebas E2E.
- Pruebas de rendimiento y estres con Locust.
- Pipelines de stage para construir, probar y desplegar en Kubernetes.
- Pipelines de master con pruebas, despliegue y Release Notes automaticas.
- Documentacion consolidada en un unico archivo.
- Guia paso a paso para ejecutar despliegue.
- Instrucciones para video de evidencia.
