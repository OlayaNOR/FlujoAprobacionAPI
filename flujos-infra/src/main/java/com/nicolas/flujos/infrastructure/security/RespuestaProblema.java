package com.nicolas.flujos.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Los errores de la cadena de filtros no pasan por el @RestControllerAdvice. */
@Component
class RespuestaProblema implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper;

    RespuestaProblema(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
        throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        escribir(request, response, HttpStatus.UNAUTHORIZED, "no-autenticado", "No autenticado",
            "Falta el token de acceso, es inválido o expiró.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
        throws IOException {
        escribir(request, response, HttpStatus.FORBIDDEN, "acceso-denegado", "Acceso denegado",
            "No tienes permisos para esta operación.");
    }

    private void escribir(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                          String tipo, String titulo, String detalle) throws IOException {
        var pd = ProblemDetail.forStatusAndDetail(status, detalle);
        pd.setType(URI.create("https://flujos.api/errores/" + tipo));
        pd.setTitle(titulo);
        pd.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getOutputStream().write(mapper.writeValueAsBytes(pd));
    }
}
