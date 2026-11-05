package com.nicolas.flujos.application.port.in;

public interface DecidirSolicitudUseCase {

    SolicitudDetalle ejecutar(DecidirSolicitudCommand comando);
}
