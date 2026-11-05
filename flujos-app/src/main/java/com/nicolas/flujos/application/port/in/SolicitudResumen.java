package com.nicolas.flujos.application.port.in;

import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.shared.Periodo;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.time.Instant;

public record SolicitudResumen(SolicitudId id, UsuarioId solicitanteId, TipoSolicitud tipo, EstadoSolicitud estado,
                               Money monto, Periodo periodo, int pasoActual, Instant creadaEn) {

    public static SolicitudResumen de(Solicitud s) {
        return new SolicitudResumen(s.getId(), s.getSolicitanteId(), s.getTipo(), s.getEstado(), s.getMonto(),
            s.getPeriodo(), s.getPasoActual(), s.getCreadaEn());
    }
}
