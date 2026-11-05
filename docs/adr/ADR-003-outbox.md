# ADR-003: Outbox en lugar de publicación directa

## Contexto

Los cambios de estado generan eventos (enviada, aprobada, rechazada, escalada) para notificar a otras partes. Si el caso de uso publicara directamente, podría publicar un evento de un cambio que después se revierte, o confirmar el cambio y caerse antes de publicar.

## Decisión

- El caso de uso guarda la solicitud y escribe sus eventos en la tabla `outbox` en la misma transacción. Si algo falla, no queda ni el cambio ni el evento.
- Un job lee cada 2 segundos hasta 50 filas sin publicar, en orden, con `FOR UPDATE SKIP LOCKED`, las entrega a `PublicadorEventos` y las marca con `publicado_en`.
- En la v1 el publicador escribe en el log. Cambiarlo por una cola es otra implementación de esa interfaz.
- El `payload` se arma campo por campo y no serializando el record del dominio, para que renombrar algo en el dominio no cambie el contrato con quien lo consuma.

## Consecuencias

- La entrega es "al menos una vez": si la aplicación cae después de publicar y antes de confirmar, el lote se publica de nuevo. Quien consuma los eventos tiene que tolerar duplicados.
- Si un evento falla al publicarse, el lote se corta ahí para no publicar fuera de orden, y los anteriores quedan marcados.
- Con varias instancias, cada una toma filas distintas gracias a `SKIP LOCKED`, sin necesidad de ShedLock.
- La tabla crece: las filas publicadas no se borran todavía.
- Hay hasta 2 segundos de demora entre el cambio y la publicación.
