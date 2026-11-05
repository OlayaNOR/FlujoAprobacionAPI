# ADR-008: Rate limiting del login en memoria

## Contexto

El login es la puerta para adivinar contraseñas. Hacen falta dos límites distintos: uno por IP, contra alguien que prueba muchas cuentas desde una máquina, y uno por email, contra alguien que prueba muchas claves de una cuenta desde muchas máquinas.

## Decisión

- **Por IP:** 5 intentos por minuto en `POST /auth/login`, cuenten o no como fallidos. Lo aplica un filtro antes de llegar al controlador. `/auth/refresh` no tiene límite porque el refresh token es aleatorio de 256 bits y no se puede adivinar.
- **Por email:** 10 fallos por hora. Después, el login con ese email responde 429 aunque la clave sea correcta, hasta recuperar un intento (uno cada 6 minutos). Un login correcto reinicia la cuenta.
- El límite por email vive detrás del puerto `IntentosLogin`. Se aplica al email normalizado, exista o no la cuenta, así que el bloqueo no revela qué cuentas existen. Se revisa antes de BCrypt, para que un email bloqueado no cueste un hash en cada intento.
- Los dos usan Bucket4j con cubetas en memoria, en un caché que descarta las que no se usan.
- La respuesta es 429 con `Retry-After` en segundos, redondeado hacia arriba.

## Consecuencias

- **Con varias instancias cada una cuenta por su lado,** así que el límite real se multiplica por el número de instancias, y un reinicio borra los contadores. Para escalar horizontalmente habría que guardar las cubetas en Redis. Bucket4j lo soporta sin cambiar el puerto.
- Cualquiera que sepa un email puede bloquearlo hasta una hora fallando a propósito. Es el costo conocido del bloqueo por cuenta. Lo acota el límite por IP, y el bloqueo se levanta solo.
- Se usa la IP de la conexión. Detrás de un balanceador todos llegarían con la misma IP, y habría que configurar `server.forward-headers-strategy` confiando solo en ese proxy. Si se confiara en `X-Forwarded-For` sin más, cualquiera podría cambiar de IP en cada intento.
