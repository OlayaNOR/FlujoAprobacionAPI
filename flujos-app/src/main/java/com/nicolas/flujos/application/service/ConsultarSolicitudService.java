package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.in.ConsultarSolicitudUseCase;
import com.nicolas.flujos.application.port.in.SolicitudDetalle;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ConsultarSolicitudService implements ConsultarSolicitudUseCase {

    private final AccesoSolicitudes acceso;

    ConsultarSolicitudService(AccesoSolicitudes acceso) {
        this.acceso = acceso;
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudDetalle ejecutar(SolicitudId id, Actor actor) {
        return SolicitudDetalle.de(acceso.cargarVisible(id, actor));
    }
}
