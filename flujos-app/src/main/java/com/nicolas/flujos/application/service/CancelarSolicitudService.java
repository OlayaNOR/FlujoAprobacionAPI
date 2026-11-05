package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.in.CancelarSolicitudUseCase;
import com.nicolas.flujos.application.port.in.SolicitudDetalle;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CancelarSolicitudService implements CancelarSolicitudUseCase {

    private final AccesoSolicitudes acceso;
    private final SolicitudRepository solicitudes;
    private final Clock clock;

    CancelarSolicitudService(AccesoSolicitudes acceso, SolicitudRepository solicitudes, Clock clock) {
        this.acceso = acceso;
        this.solicitudes = solicitudes;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SolicitudDetalle ejecutar(SolicitudId id, Actor actor) {
        var solicitud = acceso.cargarVisible(id, actor);
        solicitud.cancelar(actor.id(), clock.instant());
        solicitudes.guardar(solicitud);
        return SolicitudDetalle.de(solicitud);
    }
}
