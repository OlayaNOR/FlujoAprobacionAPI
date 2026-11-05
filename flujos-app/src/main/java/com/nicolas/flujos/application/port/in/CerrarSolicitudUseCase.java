package com.nicolas.flujos.application.port.in;

import com.nicolas.flujos.domain.solicitud.SolicitudId;

public interface CerrarSolicitudUseCase {

    SolicitudDetalle ejecutar(SolicitudId id, Actor actor);
}
