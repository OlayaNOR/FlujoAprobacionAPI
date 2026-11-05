# ADR-006: 404 en lugar de 403 para recursos ajenos

## Contexto

Con 403, un empleado podría probar IDs y distinguir cuáles existen. Los UUID hacen esto difícil, pero la respuesta seguiría revelando información que no le corresponde.

## Decisión

Quien no puede ver una solicitud (no es el solicitante, ni el aprobador asignado, ni ADMIN) recibe el mismo 404 que si no existiera.

El 403 queda para quien sí puede verla pero no puede hacer esa operación: por ejemplo, un ADMIN que intenta editar una solicitud ajena. Ahí no se revela nada que no sepa, y un 404 sería confuso.

## Consecuencias

- Toda la verificación pasa por `AccesoSolicitudes`, para que ningún caso de uso se salte la regla.
- En los logs de acceso, un 404 no distingue entre un ID que no existe y uno ajeno.
- Si el aprobador asignado cambia, el anterior deja de ver la solicitud. Eso cambiará cuando exista la auditoría de participantes.
