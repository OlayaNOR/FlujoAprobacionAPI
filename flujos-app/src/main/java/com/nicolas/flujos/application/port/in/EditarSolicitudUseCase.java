package com.nicolas.flujos.application.port.in;

public interface EditarSolicitudUseCase {

    SolicitudDetalle ejecutar(EditarSolicitudCommand comando);
}
