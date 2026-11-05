# ADR-004: Sub-recursos de acción

## Contexto

Enviar o cancelar una solicitud no es editar un campo: tiene reglas, cambia de aprobador y genera eventos. La alternativa REST más pura es un `PATCH` con `{"estado": "CANCELADA"}`.

## Decisión

Exponer cada acción como un POST sobre un sub-recurso nombrado con un sustantivo: `/solicitudes/{id}/envio` y `/solicitudes/{id}/cancelacion`. Las decisiones de aprobación van en `/solicitudes/{id}/decisiones`, porque ahí sí hay un recurso con datos propios.

## Consecuencias

- Cada acción tiene su propio permiso, sus errores y su documentación en OpenAPI.
- El cliente no puede intentar transiciones que no existen, como pasar directo a `APROBADA`. Con un `PATCH` habría que rechazarlas una por una.
- No es REST estricto. Se acepta a cambio de un contrato más fácil de leer.
