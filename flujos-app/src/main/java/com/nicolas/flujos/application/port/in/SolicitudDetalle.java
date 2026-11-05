package com.nicolas.flujos.application.port.in;

import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.shared.Periodo;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.time.Instant;

public record SolicitudDetalle(SolicitudId id, UsuarioId solicitanteId, TipoSolicitud tipo, EstadoSolicitud estado,
                               Money monto, Periodo periodo, String motivo, int pasoActual,
                               UsuarioId aprobadorActualId, Instant fechaLimitePaso, Instant creadaEn,
                               Instant actualizadaEn) {

    public static SolicitudDetalle de(Solicitud s) {
        return new SolicitudDetalle(s.getId(), s.getSolicitanteId(), s.getTipo(), s.getEstado(), s.getMonto(),
            s.getPeriodo(), s.getMotivo(), s.getPasoActual(), s.getAprobadorActualId(), s.getFechaLimitePaso(),
            s.getCreadaEn(), s.getActualizadaEn());
    }
}
