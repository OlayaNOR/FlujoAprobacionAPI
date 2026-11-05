package com.nicolas.flujos.application.port.in;

public interface CrearSolicitudUseCase {

    /** @param claveIdempotencia opcional; con null la petición siempre crea una solicitud */
    SolicitudDetalle ejecutar(CrearSolicitudCommand comando, String claveIdempotencia);
}
