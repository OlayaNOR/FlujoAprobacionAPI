package com.nicolas.flujos.api.dto;

import com.nicolas.flujos.domain.auditoria.AccionAuditoria;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** {@code actorId} viene en null cuando la acción la hizo el sistema. */
public record RegistroAuditoriaResponse(AccionAuditoria accion, UUID actorId, EstadoSolicitud estadoAnterior,
                                        EstadoSolicitud estadoNuevo, Map<String, Object> detalle,
                                        Instant ocurridoEn) {
}
