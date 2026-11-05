# ADR-011: Idempotencia en la creación de solicitudes

## Contexto

Si un cliente no recibe la respuesta de un `POST /solicitudes` y reintenta, se crea una solicitud duplicada. El encabezado `Idempotency-Key` lo evita, pero hay que definir tres cosas: dónde se aplica, qué pasa con dos peticiones simultáneas y qué responder si la clave llega con otro contenido.

## Decisión

- **Se aplica en el servicio, no en un filtro.** La reserva de la clave y la solicitud se guardan en la misma transacción. Si la creación falla, la clave queda libre y el cliente puede corregir la petición y reintentar con ella.
- **La reserva es un `INSERT ... ON CONFLICT DO NOTHING`.** La segunda petición espera a que termine la primera y después lee la respuesta guardada. Capturar la violación de clave única no serviría, porque en PostgreSQL ese error aborta la transacción y ya no se puede leer nada en ella.
- **La huella se calcula sobre el comando y no sobre el JSON.** El orden de las llaves o escribir `100` en vez de `100.00` no cambian lo que se pide.
- **La clave pertenece al usuario.** La misma clave enviada por otro usuario crea otra solicitud y no ve la respuesta ajena.
- **La respuesta se guarda como quedó al crear.** Un reintento devuelve 201 con esos datos aunque la solicitud haya avanzado después.
- **Una clave reutilizada con otro contenido es 422.** Repetir la petición igual siempre va a fallar, sin importar el estado de ningún recurso, que es el criterio del [ADR-005](ADR-005-409-vs-422.md) para el 422. Consultar el recurso no le serviría al cliente, que es lo que un 409 le sugeriría.

## Consecuencias

- La tabla `idempotencia` crece sin límite: las claves todavía no vencen. Cuando exista el job de escalamiento, otro job puede borrar las que tengan más de cierto tiempo.
- Solo aplica a la creación. El resto de las acciones ya son seguras de repetir, porque la segunda vez responden 409 por el estado.
