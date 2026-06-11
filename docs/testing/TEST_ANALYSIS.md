# Analisis de pruebas

## Resultado base

Comando:

```powershell
.\gradlew.bat test
```

Resultado esperado: `BUILD SUCCESSFUL`.

## Tipos de pruebas

- Unitarias: servicios, controladores, repositorios, listeners y convertidores.
- Integracion: Auth-Identity, Form-Kafka, File-Auth, Dashboard-DB.
- E2E: autenticacion, identidad, dashboard, formularios y archivos.
- Rendimiento: Locust simula login, identidad, dashboard, archivos, formularios y gateway.
- Seguridad: OWASP ZAP baseline propuesto para stage/prod.

## Locust

```bash
python -m locust -f tests/locust_tests.py --headless --host=http://<gateway-stage-url> -u 50 -r 5 -t 5m --csv=build/locust/load
```

Metricas: promedio, P95/P99, requests/s, tasa de errores y endpoints lentos.
