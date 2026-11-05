package com.nicolas.flujos.domain.auditoria;

import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** {@code actorId} es null cuando actúa el sistema. */
public record RegistroAuditoria(SolicitudId solicitudId, AccionAuditoria accion, UsuarioId actorId,
                                EstadoSolicitud estadoAnterior, EstadoSolicitud estadoNuevo,
                                Map<String, Object> detalle, Instant ocurridoEn) {

    public RegistroAuditoria {
        Objects.requireNonNull(solicitudId);
        Objects.requireNonNull(accion);
        Objects.requireNonNull(ocurridoEn);
        detalle = detalle == null ? Map.of() : Map.copyOf(detalle);
    }

    public static RegistroAuditoria de(Solicitud solicitud, AccionAuditoria accion, UsuarioId actorId,
                                       EstadoSolicitud estadoAnterior, Map<String, Object> detalle, Instant ahora) {
        return new RegistroAuditoria(solicitud.getId(), accion, actorId, estadoAnterior, solicitud.getEstado(),
            detalle, ahora);
    }
}
