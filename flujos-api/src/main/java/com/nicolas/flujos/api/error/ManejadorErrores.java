package com.nicolas.flujos.api.error;

import com.nicolas.flujos.application.port.in.CredencialesInvalidasException;
import com.nicolas.flujos.application.port.in.RefreshTokenInvalidoException;
import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import com.nicolas.flujos.domain.solicitud.AccionNoPermitidaException;
import com.nicolas.flujos.domain.solicitud.SolicitudNoEditableException;
import com.nicolas.flujos.domain.solicitud.SolicitudNoEncontradaException;
import com.nicolas.flujos.domain.solicitud.TransicionInvalidaException;
import java.net.URI;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final String BASE_TIPO = "https://flujos.api/errores/";

    @ExceptionHandler({CredencialesInvalidasException.class, RefreshTokenInvalidoException.class})
    ResponseEntity<ProblemDetail> noAutenticado(RuntimeException ex) {
        var pd = problema(HttpStatus.UNAUTHORIZED, "credenciales-invalidas", "No autenticado", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
            .body(pd);
    }

    // @PreAuthorize lanza dentro del controlador: sin este handler caería en el de Exception y sería un 500.
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accesoDenegado(AccessDeniedException ex) {
        return problema(HttpStatus.FORBIDDEN, "acceso-denegado", "Acceso denegado",
            "No tienes permisos para esta operación.");
    }

    @ExceptionHandler(SolicitudNoEncontradaException.class)
    ProblemDetail noEncontrada(SolicitudNoEncontradaException ex) {
        return problema(HttpStatus.NOT_FOUND, "no-encontrada", "Recurso no encontrado", "Solicitud no encontrada");
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ProblemDetail reglaNegocio(ReglaNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "regla-negocio",
            "La petición viola una regla de negocio", ex.getMessage());
    }

    @ExceptionHandler(TransicionInvalidaException.class)
    ProblemDetail transicionInvalida(TransicionInvalidaException ex) {
        var pd = problema(HttpStatus.CONFLICT, "transicion-invalida",
            "Transición de estado no permitida", ex.getMessage());
        pd.setProperty("estadoActual", ex.getEstadoActual());
        return pd;
    }

    @ExceptionHandler(SolicitudNoEditableException.class)
    ProblemDetail noEditable(SolicitudNoEditableException ex) {
        var pd = problema(HttpStatus.CONFLICT, "solicitud-no-editable",
            "La solicitud no se puede editar", ex.getMessage());
        pd.setProperty("estadoActual", ex.getEstadoActual());
        return pd;
    }

    @ExceptionHandler(AccionNoPermitidaException.class)
    ProblemDetail accionNoPermitida(AccionNoPermitidaException ex) {
        return problema(HttpStatus.FORBIDDEN, "accion-no-permitida", "Acción no permitida", ex.getMessage());
    }

    // sin ETag no hay If-Match que falle, por eso 409 y no 412
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail concurrencia(OptimisticLockingFailureException ex) {
        return problema(HttpStatus.CONFLICT, "concurrencia", "Conflicto de concurrencia",
            "La solicitud fue modificada por otra operación. Vuelve a consultarla.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail errorNoControlado(Exception ex) {
        logger.error("Error no controlado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "interno", "Error interno",
            "Error interno. Reporta el traceId al equipo.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<ErrorCampo> errores = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> new ErrorCampo(e.getField(), e.getDefaultMessage()))
            .toList();
        var pd = problema(HttpStatus.BAD_REQUEST, "validacion",
            "La solicitud contiene errores de validación", null);
        pd.setProperty("errores", errores);
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        var pd = problema(HttpStatus.BAD_REQUEST, "cuerpo-invalido", "Cuerpo de la petición inválido",
            "El cuerpo no es un JSON válido o tiene un valor con el tipo incorrecto.");
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        var pd = problema(HttpStatus.BAD_REQUEST, "parametro-invalido", "Parámetro inválido",
            "El parámetro '%s' tiene un valor inválido.".formatted(ex.getPropertyName()));
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    private static ProblemDetail problema(HttpStatus status, String tipo, String titulo, String detalle) {
        var pd = ProblemDetail.forStatus(status);
        pd.setType(URI.create(BASE_TIPO + tipo));
        pd.setTitle(titulo);
        pd.setDetail(detalle);
        var traceId = MDC.get("traceId");
        if (traceId != null) {
            pd.setProperty("traceId", traceId);
        }
        return pd;
    }

    record ErrorCampo(String campo, String mensaje) {
    }
}
