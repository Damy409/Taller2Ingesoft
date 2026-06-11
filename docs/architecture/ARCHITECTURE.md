# Arquitectura de CircleGuard

```mermaid
flowchart TB
  Mobile[Mobile/Web App] --> Gateway[Gateway Service]
  Gateway --> Auth[Auth Service]
  Auth --> Identity[Identity Service]
  Mobile --> Form[Form Service]
  Form --> Kafka[(Kafka)]
  Kafka --> Promotion[Promotion Service]
  Promotion --> Neo4j[(Neo4j Graph)]
  Promotion --> Redis[(Redis Cache)]
  Kafka --> Notification[Notification Service]
  Dashboard[Dashboard Service] --> Promotion
  File[File Service] --> Auth
  Auth --> PgAuth[(PostgreSQL Auth)]
  Identity --> PgIdentity[(PostgreSQL Identity)]
  Form --> PgForm[(PostgreSQL Form)]
  Dashboard --> PgDash[(PostgreSQL Dashboard)]
  Prom[Prometheus/Grafana] --> Gateway
  Logs[ELK] --> Gateway
  Trace[Jaeger] --> Gateway
```

## Decisiones clave

- Microservicios Spring Boot separados por dominio.
- Persistencia poliglota: PostgreSQL para datos transaccionales, Neo4j para contactos, Redis para cache.
- Comunicacion asincrona con Kafka para eventos de formularios, promociones y notificaciones.
- Kubernetes como plataforma comun para dev/stage/prod.
- Terraform para declarar namespaces, deployments y observabilidad.
