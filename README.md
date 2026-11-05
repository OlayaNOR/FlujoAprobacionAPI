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
