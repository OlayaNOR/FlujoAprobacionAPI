# Flujos de Aprobación API

[![ci](https://github.com/OlayaNOR/FlujoAprobacionAPI/actions/workflows/ci.yml/badge.svg)](https://github.com/OlayaNOR/FlujoAprobacionAPI/actions/workflows/ci.yml)

API REST para gestionar solicitudes internas (vacaciones, compras, gastos) con aprobaciones por niveles, auditoría y escalamiento por tiempo.

Lo que no está implementado, y por qué, está en [qué falta](#qué-falta).

## Módulos

| Módulo | Contenido | Depende de |
|---|---|---|
| `flujos-domain` | Agregados y value objects. Sin frameworks. | - |
| `flujos-app` | Casos de uso y puertos. | domain |
| `flujos-infra` | JPA, seguridad, jobs. | app |
| `flujos-api` | Controladores y arranque de la aplicación. | app (infra solo en runtime) |

## Compilar y probar

```bash
./mvnw verify
```

Necesita Docker corriendo: las pruebas de integración levantan PostgreSQL con Testcontainers.

| Pruebas | Dónde | Qué cubren |
|---|---|---|
| Unitarias | `flujos-domain`, `flujos-app` | Estados, reglas de los flujos, casos de uso con repositorios simulados |
| Integración | `flujos-infra`, `flujos-api` (`*IT`) | Repositorios, migraciones, seguridad, errores y concurrencia contra PostgreSQL real |
| Arquitectura | `ArquitecturaTest` | Que el dominio no use frameworks, qué partes de Spring entran a la aplicación y que los controladores pasen por los casos de uso |
| Contrato | `ContratoOpenApiIT` | Que el OpenAPI generado sea igual al guardado ([ADR-014](docs/adr/ADR-014-contrato-openapi.md)) |

El build falla si la cobertura de líneas de `flujos-domain` o `flujos-app` baja de 80%. El reporte queda en `<módulo>/target/site/jacoco/index.html`. En la API y la infraestructura no hay mínimo: ahí lo que importa lo cubren las pruebas de integración.

El CI de GitHub Actions corre lo mismo en cada push a `main` y en cada pull request, y después construye la imagen de Docker.

## Levantar con Docker

```bash
cp .env.example .env   # completar contraseñas y JWT_SECRET
docker compose up --build
```

La API queda en `http://localhost:8080`, la documentación en [`/swagger-ui.html`](http://localhost:8080/swagger-ui.html) y el estado en `/actuator/health`.

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

El login acepta 5 intentos por minuto desde una misma IP, y después de 10 fallidos en una hora con el mismo email lo bloquea un rato. En los dos casos responde 429 con `Retry-After` ([ADR-008](docs/adr/ADR-008-rate-limiting-en-memoria.md)).

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

## Escalamiento

Cada 10 minutos un job revisa las solicitudes pendientes cuyo paso venció. Si hay un paso siguiente, la solicitud avanza a él; si era el último, se queda donde está y se publica un evento para avisar al ADMIN. Las dos cosas quedan en la auditoría (`ESCALADA` y `ESCALADA_ADMIN`). El detalle está en el [ADR-013](docs/adr/ADR-013-jobs-con-varias-instancias.md).

## Flujos

Cada tipo de solicitud tiene un flujo con sus pasos de aprobación, que un ADMIN puede cambiar sin desplegar.

| Método | Ruta | Qué hace | Quién puede |
|---|---|---|---|
| GET | `/flujos/{tipo}` | Muestra los pasos configurados | Cualquier usuario autenticado |
| PUT | `/flujos/{tipo}` | Reemplaza los pasos | ADMIN |

```bash
curl -X PUT localhost:8080/api/v1/flujos/COMPRA \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"pasos":[
        {"rolAprobador":"JEFE","tiempoLimiteHoras":48},
        {"rolAprobador":"GERENTE","umbralMinimo":5000,"tiempoLimiteHoras":24}
      ]}'
```

- El orden de los pasos es su posición en la lista.
- `umbralMinimo` indica desde qué valor aplica el paso: un monto en compras y gastos, y días en vacaciones. Sin umbral, el paso aplica siempre.
- Si ningún paso aplica, la solicitud se aprueba sola. Así funcionan los gastos menores a 200.
- Una solicitud copia sus pasos al enviarse, así que un cambio solo afecta a las que se envíen después ([ADR-012](docs/adr/ADR-012-flujos-configurables.md)).

Los flujos iniciales son:

| Tipo | Pasos |
|---|---|
| `VACACIONES` | Jefe; RRHH desde 4 días |
| `COMPRA` | Jefe; gerente desde 1.000; director desde 10.000,01 |
| `GASTO` | Jefe desde 200 |

## Observabilidad

- `/actuator/health` es público. `/actuator/metrics` y `/actuator/info` exigen el rol ADMIN.
- Métricas propias: `solicitudes.creadas` (por tipo), `solicitudes.por_estado` (gauge, se actualiza cada minuto) y `aprobaciones.tiempo_respuesta` (timer por tipo y resultado, desde que el paso le llega al aprobador hasta que decide). Se registran al confirmar la transacción.
- Los logs salen en JSON salvo en el perfil `local`, e incluyen `traceId` y `spanId`. El mismo `traceId` aparece en las respuestas de error, para encontrar el log de una petición que falló.

## Documentación de la API

Swagger UI está en `/swagger-ui.html` y el OpenAPI en `/v3/api-docs`. Cada endpoint muestra sus errores posibles con un ejemplo. Para probar desde ahí: hacer login, copiar el `accessToken` y pegarlo en **Authorize**.

Si un cambio modifica el contrato a propósito, la prueba de contrato falla hasta regenerar el archivo guardado:

```bash
./mvnw verify -Dcontrato.actualizar=true
```

## Colección de Bruno

En [`bruno/`](bruno) está la colección para [Bruno](https://www.usebruno.com/), con el entorno `local`. Hace el login de cada rol, una compra que pasa por los tres niveles hasta el cierre, un gasto que se aprueba solo, unas vacaciones rechazadas y algunos errores (422, 409 y 404). Cada petición tiene pruebas de su respuesta.

Desde la línea de comandos, con la app corriendo:

```bash
cd bruno && npx @usebruno/cli run -r --env local
```

La colección hace cinco logins. Si se corre dos veces en menos de un minuto, el límite por IP responde 429 en la segunda.

## Diagramas y decisiones

- [Estados de una solicitud](docs/diagramas/estados.md) y [componentes](docs/diagramas/componentes.md), en Mermaid.
- [Decisiones de arquitectura](docs/adr), del ADR-001 al ADR-014.

## Qué falta

- **Demo desplegada.** Todavía no hay una instancia pública.
- **Auditoría de cambios a los flujos.** Solo queda una línea en el log con quién cambió qué ([ADR-012](docs/adr/ADR-012-flujos-configurables.md)).
- **Activar o desactivar un flujo.** El campo `activo` existe, pero solo se puede cambiar en la base de datos.
- **Límites del login con varias instancias.** Los contadores viven en memoria, así que cada instancia lleva su propia cuenta ([ADR-008](docs/adr/ADR-008-rate-limiting-en-memoria.md)).
- **Limpieza de tablas.** Las filas ya publicadas del outbox y las claves de idempotencia viejas no se borran.
- **Notificaciones reales.** Los eventos se publican en el log; un adaptador de correo o de cola se conecta en `PublicadorEventos` sin tocar los casos de uso.
