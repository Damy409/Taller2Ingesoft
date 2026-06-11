# Change Management y Release Notes

## Flujo formal

1. Crear issue/historia con alcance, riesgo y criterios de aceptacion.
2. Crear rama `feature/*` o `hotfix/*`.
3. Ejecutar pruebas locales.
4. Pull Request hacia `develop` para stage.
5. Pipeline stage ejecuta pruebas, Trivy, Sonar, ZAP smoke y despliegue stage.
6. Aprobacion manual para produccion.
7. Merge/tag hacia `master/main`.
8. Pipeline master genera release notes, despliega y archiva evidencia.

## Versionado semantico

MAJOR.MINOR.PATCH con tags `v1.0.0`, `v1.1.0`, `v1.1.1`.

## Release notes automaticas

```bash
mkdir -p build/release-notes
git log --pretty=format:'- %h %s (%an)' > build/release-notes/release.md
```

## Rollback

```bash
kubectl rollout undo deployment/<service> -n circleguard-master
```

