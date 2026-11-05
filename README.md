# Flujos de Aprobación API

API REST para gestionar solicitudes internas (vacaciones, compras, gastos) con aprobaciones por niveles, auditoría y escalamiento por tiempo.

Proyecto en desarrollo. Ver [qué falta](#qué-falta).

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

## Levantar con Docker

```bash
cp .env.example .env   # completar contraseñas y JWT_SECRET
docker compose up --build
```

La API queda en `http://localhost:8080` y el estado en `/actuator/health`.

El contenedor de PostgreSQL usa `flujos_migrador` como superusuario para simplificar el entorno local. En producción ese usuario solo debería tener permisos de DDL sobre el esquema.

## Autenticación

Compose arranca con el perfil `local`, que carga usuarios de ejemplo. Todos usan la contraseña `demo-flujos-2026`.

| Usuario | Roles | Cargo | Jefe directo |
|---|---|---|---|
| `admin@flujos.test` | ADMIN | - | - |
| `directora@flujos.test` | APROBADOR, EMPLEADO | DIRECTOR | - |
| `gerente@flujos.test` | APROBADOR, EMPLEADO | GERENTE | directora |
| `rrhh@flujos.test` | APROBADOR, EMPLEADO | RRHH | directora |
| `jefe@flujos.test` | APROBADOR, EMPLEADO | JEFE | gerente |
| `empleado@flujos.test` | EMPLEADO | - | jefe |
| `empleada@flujos.test` | EMPLEADO | - | jefe |

```bash
curl -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"empleado@flujos.test","password":"demo-flujos-2026"}'
```

El access token dura 15 minutos. Para renovarlo se envía el refresh token a `POST /api/v1/auth/refresh`, que entrega un par nuevo e invalida el anterior.

## Solicitudes

Todas las rutas van bajo `/api/v1` y exigen `Authorization: Bearer <accessToken>`.

| Método | Ruta | Qué hace | Quién puede |
|---|---|---|---|
| POST | `/solicitudes` | Crea en `BORRADOR` | Rol EMPLEADO |
| GET | `/solicitudes` | Lista con filtros | Cada uno ve lo suyo y lo que tiene por aprobar; ADMIN ve todo |
| GET | `/solicitudes/{id}` | Detalle | Solicitante, aprobador asignado, ADMIN |
| PUT | `/solicitudes/{id}` | Edita un borrador | Solicitante |
| POST | `/solicitudes/{id}/envio` | Envía a aprobación | Solicitante |
| POST | `/solicitudes/{id}/decisiones` | Aprueba o rechaza el paso actual | Aprobador asignado |
| POST | `/solicitudes/{id}/cancelacion` | Cancela | Solicitante |
| POST | `/solicitudes/{id}/cierre` | Cierra una solicitud aprobada | ADMIN |
| GET | `/solicitudes/{id}/auditoria` | Historial de transiciones | Quien puede ver la solicitud |

Quien no puede ver una solicitud recibe 404 aunque exista ([ADR-006](docs/adr/ADR-006-404-para-recursos-ajenos.md)).

Filtros de `GET /solicitudes`: `estado` (se puede repetir), `tipo`, `desde`, `hasta`, `page`, `size` (máximo 100) y `sort` (`creadaEn`, `monto` o `estado`, con `,asc` o `,desc`).

```bash
curl -X POST localhost:8080/api/v1/solicitudes \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"tipo":"COMPRA","monto":{"valor":1250.50,"moneda":"USD"},"motivo":"Licencias"}'
```

`POST /solicitudes` acepta el encabezado opcional `Idempotency-Key`. Repetir la petición con la misma clave y el mismo contenido devuelve la respuesta original sin crear otra solicitud, y usarla con otro contenido da 422. El detalle está en el [ADR-011](docs/adr/ADR-011-idempotencia-en-la-creacion.md).

Los errores usan `application/problem+json`. El criterio entre 409 y 422 está en el [ADR-005](docs/adr/ADR-005-409-vs-422.md).

## Qué falta

- Publicación de eventos con outbox: hoy el dominio los genera, pero no se guardan.
- Job de escalamiento, rate limiting en el login, métricas y documentación OpenAPI.
