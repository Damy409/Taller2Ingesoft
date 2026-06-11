# Metodologia agil y branching

## Metodologia

Se adopto Scrum ligero con tablero Kanban. Cada sprint dura una semana y termina con demo tecnica.

## Herramienta de gestion

Tablero sugerido: GitHub Projects, Trello o Jira con columnas: Backlog, Ready, In Progress, Review, Done.

## Estrategia de branching

- `master`: codigo candidato a produccion.
- `develop`: integracion para stage.
- `feature/<descripcion>`: trabajo por historia.
- `hotfix/<descripcion>`: correcciones urgentes.
- Tags `vMAJOR.MINOR.PATCH`: releases aprobados.

## Iteracion 1

Objetivo: estabilizar microservicios, pruebas y despliegue base.

Historias:

1. Como operador quiero ejecutar pruebas unitarias/integracion/E2E para detectar regresiones.
   - Criterios: `./gradlew test` finaliza exitosamente y reportes JUnit disponibles.
2. Como DevOps quiero contenedores por servicio para desplegar en Kubernetes.
   - Criterios: Dockerfile por microservicio e imagen auth validada localmente.
3. Como administrador quiero ver pods y logs en Kubernetes.
   - Criterios: `kubectl apply -f k8s/`, pod auth Running y logs evidencian arranque.

## Iteracion 2

Objetivo: completar DevOps avanzado, seguridad, observabilidad e IaC.

Historias:

1. Como DevOps quiero Terraform modular multiambiente.
   - Criterios: `infra/terraform/modules`; `envs/dev`, `stage`, `prod`; backend remoto documentado.
2. Como lider tecnico quiero patrones de resiliencia y configuracion.
   - Criterios: Circuit Breaker implementado; external config y feature toggle documentados.
3. Como equipo quiero monitoreo, logs y tracing.
   - Criterios: manifiestos Prometheus, Grafana, ELK y Jaeger.
4. Como seguridad quiero RBAC, secretos, TLS y escaneo continuo.
   - Criterios: manifests RBAC/NetworkPolicy/TLS; Trivy y ZAP en pipeline.
