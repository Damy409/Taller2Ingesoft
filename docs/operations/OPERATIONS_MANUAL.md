# Manual de operaciones basico

## Build y pruebas

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

## Docker

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

## Reinicio controlado

```powershell
kubectl rollout restart deployment/circleguard-auth-service
kubectl rollout status deployment/circleguard-auth-service
```

## Rollback

```powershell
kubectl rollout undo deployment/circleguard-auth-service -n circleguard-master
```

## Monitoreo

```powershell
kubectl apply -f k8s\observability\
kubectl -n observability get pods
```

## Incidentes comunes

- Pod en CrashLoopBackOff: revisar `kubectl logs` y variables de entorno.
- Imagen no encontrada: verificar `docker images` o registry.
- Servicio no responde: revisar `kubectl get svc`, endpoints y readiness probe.
