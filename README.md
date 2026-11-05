# Flujos de Aprobación API

API REST para gestionar solicitudes internas (vacaciones, compras, gastos) con aprobaciones por niveles, auditoría y escalamiento por tiempo.

Proyecto en desarrollo.

## Módulos

| Módulo | Contenido | Depende de |
|---|---|---|
| `flujos-domain` | Agregados, value objects y reglas. Sin frameworks. | - |
| `flujos-app` | Casos de uso y puertos. | domain |
| `flujos-infra` | JPA, seguridad, jobs. | app |
| `flujos-api` | Controladores y arranque de la aplicación. | app (infra solo en runtime) |

## Compilar

```bash
./mvnw verify
```
