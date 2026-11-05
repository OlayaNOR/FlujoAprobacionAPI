# ADR-009: Aprobador asignado como columna de la solicitud

## Contexto

Solo el aprobador del paso actual puede decidir, y el listado de `GET /solicitudes` debe mostrarle a cada aprobador lo que tiene por resolver. Con solo el número de paso y el rol habría que recalcular el aprobador en cada consulta, y la tabla `aprobacion` no sirve porque guarda decisiones pasadas, no la pendiente.

## Decisión

Guardar `aprobador_actual_id` en `solicitud`. El agregado lo actualiza al enviar, aprobar o escalar, usando un `AsignadorAprobador` que recibe por parámetro. Queda en `null` cuando no hay nada pendiente.

## Consecuencias

- El filtro del listado es una comparación simple, cubierta por un índice parcial.
- Si el jefe de alguien cambia con una solicitud en curso, esa solicitud sigue asignada al anterior hasta que avance o se escale.
- Es un dato desnormalizado: solo el agregado puede cambiarlo.
