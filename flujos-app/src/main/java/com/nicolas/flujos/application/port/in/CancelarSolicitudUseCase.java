package com.nicolas.flujos.application.port.in;

import com.nicolas.flujos.domain.solicitud.SolicitudId;

public interface CancelarSolicitudUseCase {

    SolicitudDetalle ejecutar(SolicitudId id, Actor actor);
}
