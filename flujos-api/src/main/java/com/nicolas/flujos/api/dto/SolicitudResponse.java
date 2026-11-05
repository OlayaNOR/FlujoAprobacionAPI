package com.nicolas.flujos.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SolicitudResponse(UUID id, UUID solicitanteId, TipoSolicitud tipo, EstadoSolicitud estado,
                                Monto monto, Periodo periodo, String motivo, int pasoActual,
                                UUID aprobadorActualId, Instant fechaLimitePaso, Instant creadaEn,
                                Instant actualizadaEn) {

    public record Monto(BigDecimal valor, String moneda) {
    }

    public record Periodo(LocalDate inicio, LocalDate fin) {
    }
}
