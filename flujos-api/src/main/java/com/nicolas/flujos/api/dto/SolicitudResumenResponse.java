package com.nicolas.flujos.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SolicitudResumenResponse(UUID id, UUID solicitanteId, TipoSolicitud tipo, EstadoSolicitud estado,
                                       SolicitudResponse.Monto monto, SolicitudResponse.Periodo periodo,
                                       int pasoActual, Instant creadaEn) {
}
