# ADR-014: Contrato OpenAPI generado desde el código

## Contexto

La API se documenta con springdoc, que genera el OpenAPI a partir de los controladores y los DTOs. La alternativa es escribir el contrato primero (un `openapi.yaml`) y generar las interfaces de los controladores con un plugin.

El riesgo de generarlo desde el código es que un cambio en un DTO o en una anotación modifique el contrato sin que nadie lo note.

## Decisión

- El contrato se genera desde el código. Con un solo equipo y sin clientes externos, escribirlo a mano duplica cada cambio sin agregar control.
- `ContratoOpenApiIT` compara el `/v3/api-docs` generado contra `flujos-api/src/test/resources/contrato/openapi.json`. Si difieren, la prueba falla.
- Para aceptar un cambio se regenera el archivo con `-Dcontrato.actualizar=true`, y el diff queda en el commit para revisarlo como cualquier otro código.
- Los errores de cada endpoint se declaran con `@ErroresPosibles({404, 409})`. Un `OperationCustomizer` arma la respuesta con el esquema `Problema` y un ejemplo, y agrega el 401 a todo lo que exige token. Escribirlos con `@ApiResponse` eran unas diez líneas por código, casi iguales en todos los endpoints.

## Consecuencias

- Un cambio de contrato no puede pasar sin tocar el archivo guardado, y se ve en el diff.
- La prueba no distingue un cambio compatible de uno que rompe clientes. Eso todavía depende de quien revisa.
- Los ejemplos de error se escriben a mano y pueden quedar desactualizados respecto a los mensajes reales.
- Swagger UI y `/v3/api-docs` son públicos. Si la API se expusiera fuera de la empresa, se pueden apagar con `springdoc.api-docs.enabled=false`.
