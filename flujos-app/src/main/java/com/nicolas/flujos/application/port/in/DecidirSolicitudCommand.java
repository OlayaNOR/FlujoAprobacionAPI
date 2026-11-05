package com.nicolas.flujos.application.port.in;

import com.nicolas.flujos.domain.solicitud.SolicitudId;

public record DecidirSolicitudCommand(SolicitudId solicitudId, Actor actor, TipoDecision tipo, String comentario) {
}
