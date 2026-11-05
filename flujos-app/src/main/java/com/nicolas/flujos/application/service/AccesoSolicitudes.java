package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.out.AprobacionRepository;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.solicitud.AccionNoPermitidaException;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import com.nicolas.flujos.domain.solicitud.SolicitudNoEncontradaException;
import org.springframework.stereotype.Component;

@Component
class AccesoSolicitudes {

    private final SolicitudRepository solicitudes;
    private final AprobacionRepository aprobaciones;

    AccesoSolicitudes(SolicitudRepository solicitudes, AprobacionRepository aprobaciones) {
        this.solicitudes = solicitudes;
        this.aprobaciones = aprobaciones;
    }

    /** Quien no puede ver la solicitud recibe lo mismo que si no existiera. Ver ADR-006. */
    Solicitud cargarVisible(SolicitudId id, Actor actor) {
        var solicitud = solicitudes.buscarPorId(id)
            .orElseThrow(() -> new SolicitudNoEncontradaException(id));
        var esDueno = solicitud.getSolicitanteId().equals(actor.id());
        if (!esDueno && !solicitud.esAprobadorDelPasoActual(actor.id()) && !actor.esAdmin()
            && !aprobaciones.participo(id, actor.id())) {
            throw new SolicitudNoEncontradaException(id);
        }
        return solicitud;
    }

    Solicitud cargarDelSolicitante(SolicitudId id, Actor actor) {
        var solicitud = cargarVisible(id, actor);
        if (!solicitud.getSolicitanteId().equals(actor.id())) {
            throw new AccionNoPermitidaException("Solo el solicitante puede hacer esta operación");
        }
        return solicitud;
    }
}
