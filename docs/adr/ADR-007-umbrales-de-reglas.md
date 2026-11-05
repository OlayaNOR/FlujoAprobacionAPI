# ADR-007: Umbrales de aprobación en código para la v1

**Reemplazado por el [ADR-012](ADR-012-flujos-configurables.md).**

## Contexto

Los umbrales (1.000 y 10.000 USD en compras, 200 USD en gastos, 3 días en vacaciones) podrían vivir en la tabla `paso_aprobacion`, que ya tiene `umbral_minimo`, o en las clases de regla.

## Decisión

En la v1 quedan como constantes en `ReglaVacaciones`, `ReglaCompra` y `ReglaGasto`. Son fáciles de probar en los bordes y el `Flujo` configurable todavía no existe.

## Consecuencias

- Cambiar un umbral exige un despliegue.
- Las reglas comparan contra USD. Una moneda base distinta necesita sus propios umbrales, y `Money` lanza una excepción en vez de comparar monedas distintas.
- Cuando exista `Flujo`, las clases de regla pueden leer de ahí sin que `Solicitud` cambie, porque solo conoce la interfaz `ReglaAprobacion`.
