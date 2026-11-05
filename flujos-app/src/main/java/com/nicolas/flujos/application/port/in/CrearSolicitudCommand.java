package com.nicolas.flujos.application.port.in;

import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.shared.Periodo;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import com.nicolas.flujos.domain.usuario.UsuarioId;

public record CrearSolicitudCommand(UsuarioId solicitanteId, TipoSolicitud tipo, Money monto, Periodo periodo,
                                    String motivo) {
}
