package com.nicolas.flujos.application.port.in;

import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.shared.Periodo;
import com.nicolas.flujos.domain.solicitud.SolicitudId;

public record EditarSolicitudCommand(SolicitudId solicitudId, Actor actor, Money monto, Periodo periodo,
                                     String motivo) {
}
