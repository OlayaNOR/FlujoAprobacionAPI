package com.nicolas.flujos.application.port.in;

public interface CrearSolicitudUseCase {

    SolicitudDetalle ejecutar(CrearSolicitudCommand comando);
}
