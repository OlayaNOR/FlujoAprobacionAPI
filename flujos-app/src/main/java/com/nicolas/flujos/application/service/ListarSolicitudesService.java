package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.in.FiltroSolicitud;
import com.nicolas.flujos.application.port.in.ListarSolicitudesUseCase;
import com.nicolas.flujos.application.port.in.Pagina;
import com.nicolas.flujos.application.port.in.PaginaSolicitada;
import com.nicolas.flujos.application.port.in.SolicitudResumen;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ListarSolicitudesService implements ListarSolicitudesUseCase {

    private final SolicitudRepository solicitudes;

    ListarSolicitudesService(SolicitudRepository solicitudes) {
        this.solicitudes = solicitudes;
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<SolicitudResumen> ejecutar(FiltroSolicitud filtro, PaginaSolicitada pagina, Actor actor) {
        var visiblePara = actor.esAdmin() ? null : actor.id();
        return solicitudes.listar(filtro, visiblePara, pagina).map(SolicitudResumen::de);
    }
}
