package com.nicolas.flujos.application.port.in;

public interface ListarSolicitudesUseCase {

    Pagina<SolicitudResumen> ejecutar(FiltroSolicitud filtro, PaginaSolicitada pagina, Actor actor);
}
