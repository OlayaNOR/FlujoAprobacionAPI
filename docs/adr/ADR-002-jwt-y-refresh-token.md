# ADR-002: JWT HS256 con refresh token rotativo

## Contexto

La API es stateless y se consume con bearer. Hay un solo servicio que emite y valida tokens.

## Decisión

- **Access token:** JWT HS256 de 15 minutos. Se valida firma, `exp`, `iss` y `aud`, con 30 segundos de tolerancia de reloj. Basta un secreto compartido porque emisor y validador son el mismo servicio. Si la autenticación se separa, se pasa a RS256.
- **Refresh token:** 256 bits de `SecureRandom`, no un UUID, que solo tiene 122 bits aleatorios. Dura 7 días y en la base se guarda únicamente su hash SHA-256.
- **Rotación:** cada renovación revoca el token usado y entrega otro.
- **Reutilización:** presentar un token ya revocado revoca todas las sesiones del usuario, porque significa que dos partes tienen el mismo token.

## Consecuencias

- Revocar un refresh token no invalida el access token vigente: puede seguir funcionando hasta 15 minutos.
- Si un cliente reenvía un refresh por un fallo de red, se le cierra la sesión y tiene que volver a entrar. Se prefiere ese falso positivo a dejar pasar un token robado.
- La fila se bloquea con `SELECT FOR UPDATE` durante la renovación, así dos peticiones simultáneas no pueden rotar el mismo token.
- Los tokens vencidos se acumulan en la tabla. Falta un job de limpieza.
