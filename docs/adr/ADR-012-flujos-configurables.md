# ADR-012: Flujos configurables con copia de pasos en la solicitud

## Contexto

Los umbrales estaban en clases de regla ([ADR-007](ADR-007-umbrales-de-reglas.md)), pero la API necesita un `PUT /flujos/{tipo}`. Un `PUT` que guarde datos sin cambiar el comportamiento engañaría a quien lo use. Hacerlo efectivo tiene un riesgo: `paso_actual` es un número, y si se interpreta contra el flujo vigente, cambiar el flujo correría los pasos de las solicitudes en curso.

## Decisión

- **Los pasos y umbrales viven en `flujo` y `paso_aprobacion`.** V6 los carga con los valores que tenían las reglas. Las clases de regla desaparecen, porque lo único que seguía siendo propio de cada tipo es contra qué se compara el umbral.
- **El umbral es inclusivo:** el paso aplica desde ese valor. En compras y gastos es un monto en la moneda base y en vacaciones son días enteros. Por eso el paso de director tiene 10000.01: los montos tienen dos decimales, así que equivale a "más de 10.000". Un paso sin umbral aplica siempre.
- **La solicitud copia sus pasos al enviarse,** en la columna `pasos`. Aprobar y escalar leen esa copia, así que un cambio de flujo solo afecta a las solicitudes que se envíen después. V7 les da la copia a las que ya estaban en curso.
- **El orden es la posición en la lista.** El `PUT` no recibe `orden`, y así no hay huecos ni repetidos que validar en la API.
- **Si ningún paso aplica, la solicitud se aprueba sola.** Es como funcionan los gastos menores a 200. Un flujo sin ningún paso se rechaza con 422, para que vaciar la lista no apruebe todo por accidente.
- **Dos cambios simultáneos se serializan con un bloqueo de fila** y gana el último. El `PUT` reemplaza todo y no recibe una versión esperada, así que un bloqueo optimista solo haría fallar al segundo sin ganar nada. `flujo.version` cuenta los cambios.

## Consecuencias

- Cambiar un umbral ya no exige un despliegue, y la moneda deja de estar fija en USD. Si se cambia la moneda base, hay que ajustar los umbrales de compras y gastos con otro `PUT`.
- Reduce el control en un punto: un ADMIN puede cambiar quién aprueba qué, y `registro_auditoria` solo cubre solicitudes porque exige un `solicitud_id`. Por ahora el único rastro es una línea de log. Para un entorno real haría falta un historial de flujos.
- `activo` se respeta al enviar (422 si está desactivado), pero la especificación no define un endpoint para cambiarlo. Hoy solo se cambia en la base.
- Un flujo se puede guardar con un cargo que nadie tiene. No se valida al configurarlo porque los usuarios cambian, así que el error aparece al enviar o al avanzar a ese paso (422).
