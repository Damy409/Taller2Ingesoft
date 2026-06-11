# Patrones de diseno y arquitectura

## Patrones existentes identificados

- Repository: repositorios Spring Data separan persistencia de logica de dominio.
- Service Layer: servicios encapsulan reglas como `HealthStatusService`, `IdentityVaultService` y `JwtTokenService`.
- Controller: capa REST por microservicio.
- Event-driven architecture: Kafka desacopla Form, Promotion y Notification.
- API Gateway: Gateway Service concentra validacion de acceso.

## Patrones agregados o mejorados

### Circuit Breaker

Implementado en `SimpleCircuitBreaker` y usado por `IdentityClient` del Auth Service.

Proposito: evitar que Auth falle completamente cuando Identity esta temporalmente no disponible.

Beneficios: reduce propagacion de fallos, permite fallback controlado y mejora disponibilidad percibida.

### External Configuration

Configuraciones relevantes se leen desde propiedades o variables de entorno:

- `circleguard.identity-service.url`
- `circleguard.features.identity-fallback`
- secretos JWT/QR
- URLs JDBC en Kubernetes

### Feature Toggle

`circleguard.features.identity-fallback` controla si Auth puede usar UUID deterministico de fallback cuando Identity no responde.

### Bulkhead operativo

Se separan namespaces `circleguard-dev`, `circleguard-stage`, `circleguard-prod` y `observability`, reduciendo impacto cruzado entre ambientes.
