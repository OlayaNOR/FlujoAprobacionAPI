# ADR-013: Jobs sin ShedLock y escalamiento por solicitud

## Contexto

Hay dos jobs: el relevo del outbox ([ADR-003](ADR-003-outbox.md)) y el escalamiento de solicitudes vencidas. Si la aplicación corre en varias instancias, cada una ejecuta los dos. La especificación dejaba abierta la pregunta de si hace falta ShedLock.

## Decisión

- **Sin ShedLock.** Los dos jobs trabajan fila por fila con `FOR UPDATE SKIP LOCKED`, así que dos instancias pueden correr a la vez sin procesar la misma fila: la que llega segunda salta las tomadas. ShedLock evitaría que corran a la vez, que no es el problema.
- **El escalamiento usa una transacción por solicitud.** Primero lista hasta 100 vencidas sin bloquear. Después, por cada una, la vuelve a leer con bloqueo y solo si sigue vencida, la escala y guarda auditoría y evento juntos. Si una falla, las demás siguen.
- **El escalamiento no tiene actor:** la auditoría lo registra con `actor_id` nulo, igual que la aprobación automática.
- **En el último paso la solicitud no cambia de aprobador.** Se registra `ESCALADA_ADMIN`, se publica un evento con `notificarAdmin` y se borra la fecha límite, para que el job no vuelva a avisar cada 10 minutos.

## Consecuencias

- Si un aprobador decide justo cuando el job escala, uno de los dos falla por la versión: el usuario recibe 409, o el job registra el fallo y vuelve a evaluar en la siguiente corrida.
- Si el siguiente paso no tiene a quién asignarse (nadie con ese cargo), el escalamiento falla en cada corrida y deja una advertencia en el log hasta que se corrija.
- El aprobador al que se le quitó el paso sin decidir deja de ver la solicitud ([ADR-006](ADR-006-404-para-recursos-ajenos.md)).
- Una solicitud que se avisó al ADMIN queda sin fecha límite. Sigue esperando al aprobador del último paso, y el job no vuelve a tomarla.
