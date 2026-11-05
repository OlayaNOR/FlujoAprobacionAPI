package com.nicolas.flujos.api.doc;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DocumentacionConfig {

    private static final String ESQUEMA_TOKEN = "bearer";
    private static final String PROBLEMA = "Problema";

    @Bean
    OpenAPI documentacion() {
        return new OpenAPI()
            .info(new Info()
                .title("Flujos de Aprobación API")
                .version("v1")
                .description("""
                    Solicitudes internas (vacaciones, compras y gastos) con aprobación por niveles.

                    Para probarla: hacer login con uno de los usuarios de ejemplo del README, copiar el \
                    `accessToken` y pegarlo en **Authorize**. Los errores siguen el formato RFC 7807."""))
            .components(new Components()
                .addSecuritySchemes(ESQUEMA_TOKEN, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
                .addSchemas(PROBLEMA, esquemaProblema()))
            .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_TOKEN));
    }

    // Escribir los errores en cada endpoint con @ApiResponse eran diez líneas por código, casi iguales.
    @Bean
    OperationCustomizer erroresPosibles() {
        return (operacion, metodo) -> {
            var codigos = new TreeSet<Integer>();
            var anotacion = metodo.getMethodAnnotation(ErroresPosibles.class);
            if (anotacion != null) {
                Arrays.stream(anotacion.value()).forEach(codigos::add);
            }
            var conToken = exigeToken(operacion);
            if (conToken) {
                codigos.add(401);
            }
            codigos.forEach(c -> operacion.getResponses().addApiResponse(String.valueOf(c), respuesta(c, conToken)));
            return operacion;
        };
    }

    // Los endpoints públicos llevan @SecurityRequirements() vacío: eso deja la lista en [] y no en null.
    private static boolean exigeToken(Operation operacion) {
        return operacion.getSecurity() == null || !operacion.getSecurity().isEmpty();
    }

    private static ApiResponse respuesta(int codigo, boolean conToken) {
        var ejemplo = ejemplo(codigo, conToken);
        var cuerpo = new LinkedHashMap<String, Object>();
        cuerpo.put("type", "https://flujos.api/errores/" + ejemplo.tipo());
        cuerpo.put("title", ejemplo.titulo());
        cuerpo.put("status", codigo);
        if (ejemplo.detalle() != null) {
            cuerpo.put("detail", ejemplo.detalle());
        }
        cuerpo.put("traceId", "6744d1f3b0c2a9e15f0d8c7b2a4e9f10");
        cuerpo.putAll(ejemplo.extra());

        var respuesta = new ApiResponse()
            .description(ejemplo.descripcion())
            .content(new Content().addMediaType("application/problem+json",
                new MediaType().schema(new Schema<>().$ref(PROBLEMA)).example(cuerpo)));
        if (codigo == 429) {
            respuesta.addHeaderObject("Retry-After",
                new Header().description("Segundos que hay que esperar").schema(new IntegerSchema()));
        }
        return respuesta;
    }

    private static Ejemplo ejemplo(int codigo, boolean conToken) {
        return switch (codigo) {
            case 400 -> new Ejemplo("Datos inválidos o mal formados", "validacion",
                "La solicitud contiene errores de validación", null,
                Map.of("errores", List.of(Map.of("campo", "monto.valor",
                    "mensaje", "must be greater than or equal to 0.01"))));
            case 401 -> conToken
                ? new Ejemplo("Falta el token, es inválido o expiró", "no-autenticado", "No autenticado",
                    "Falta el token de acceso, es inválido o expiró.", Map.of())
                : new Ejemplo("Credenciales o refresh token inválidos", "credenciales-invalidas", "No autenticado",
                    "Email o contraseña incorrectos", Map.of());
            case 403 -> new Ejemplo("El usuario no tiene permiso para la operación", "acceso-denegado",
                "Acceso denegado", "No tienes permisos para esta operación.", Map.of());
            case 404 -> new Ejemplo("No existe, o existe pero el usuario no puede verla", "no-encontrada",
                "Recurso no encontrado", "Solicitud no encontrada", Map.of());
            case 409 -> new Ejemplo("El estado actual no permite la acción, o alguien la modificó al mismo tiempo",
                "transicion-invalida", "Transición de estado no permitida",
                "La solicitud está en APROBADA y no puede pasar a CANCELADA.", Map.of("estadoActual", "APROBADA"));
            case 422 -> new Ejemplo("La petición viola una regla de negocio", "regla-negocio",
                "La petición viola una regla de negocio", "La moneda debe ser USD", Map.of());
            case 429 -> new Ejemplo("Demasiados intentos de login", "demasiados-intentos", "Demasiados intentos",
                "Demasiados intentos de login desde esta dirección. Espera antes de volver a intentar.", Map.of());
            default -> throw new IllegalArgumentException("No hay ejemplo para el código " + codigo);
        };
    }

    private static Schema<?> esquemaProblema() {
        var errorCampo = new ObjectSchema()
            .addProperty("campo", new StringSchema())
            .addProperty("mensaje", new StringSchema());
        return new ObjectSchema()
            .description("Error en formato RFC 7807")
            .addProperty("type", new StringSchema().format("uri"))
            .addProperty("title", new StringSchema())
            .addProperty("status", new IntegerSchema())
            .addProperty("detail", new StringSchema())
            .addProperty("instance", new StringSchema())
            .addProperty("traceId", new StringSchema().description("Para buscar la petición en los logs"))
            .addProperty("estadoActual", new StringSchema().description("Solo en los 409 por estado"))
            .addProperty("errores", new ArraySchema().items(errorCampo).description("Solo en los 400 de validación"));
    }

    private record Ejemplo(String descripcion, String tipo, String titulo, String detalle, Map<String, Object> extra) {
    }
}
