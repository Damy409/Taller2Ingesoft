# Costos de infraestructura estimados

| Componente | Dev | Stage | Prod | Nota |
|---|---:|---:|---:|---|
| Kubernetes nodes | bajo/local | medio | alto | Prod requiere HA |
| PostgreSQL | bajo | medio | alto | Backups y replicas en prod |
| Neo4j | bajo | medio | alto | Alto consumo si crece el grafo |
| Kafka | medio | medio | alto | Requiere storage persistente |
| Observabilidad | bajo | medio | medio/alto | Prometheus, Grafana, ELK, Jaeger |
| Registry/CI | bajo | medio | medio | Jenkins + Docker registry |

Para demo academica se recomienda Docker Desktop o Minikube. Para produccion real se recomienda cluster administrado y bases de datos gestionadas.
