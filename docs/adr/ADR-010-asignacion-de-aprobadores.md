# ADR-010: Asignación de aprobadores por jefe directo y cargo

## Contexto

Los flujos piden pasos de `JEFE`, `RRHH`, `GERENTE` y `DIRECTOR`, pero los roles de seguridad solo son `EMPLEADO`, `APROBADOR` y `ADMIN`. Hacía falta saber quién aprueba cada paso. Asignar solo por cargo mandaría solicitudes a jefes de otros equipos.

## Decisión

Agregar `jefe_id` y `cargo` a `usuario`.

- El paso `JEFE` va al jefe directo del solicitante.
- Los demás pasos van al usuario activo más antiguo con ese cargo, sin contar al solicitante.
- En ambos casos el usuario debe estar activo y tener el rol `APROBADOR`. Si no hay nadie, enviar falla con 422.

El rol dice qué puede hacer un usuario en la API. El cargo dice qué lugar ocupa en la empresa.

## Consecuencias

- Un solicitante nunca recibe su propia solicitud en un paso por cargo, y la base impide que alguien sea su propio jefe.
- Con varios usuarios en un mismo cargo no hay reparto de carga: todo le llega al más antiguo. Es suficiente para la v1.
- Los jefes no se validan contra ciclos (A jefe de B y B jefe de A). No afecta la asignación porque solo se mira un nivel.
