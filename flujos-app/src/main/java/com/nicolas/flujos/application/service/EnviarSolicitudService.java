package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.in.EnviarSolicitudUseCase;
import com.nicolas.flujos.application.port.in.SolicitudDetalle;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.regla.AsignadorAprobador;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class EnviarSolicitudService implements EnviarSolicitudUseCase {

    private final AccesoSolicitudes acceso;
    private final SolicitudRepository solicitudes;
    private final ReglasAprobacionRegistry reglas;
    private final AsignadorAprobador asignador;
    private final Clock clock;

    EnviarSolicitudService(AccesoSolicitudes acceso, SolicitudRepository solicitudes, ReglasAprobacionRegistry reglas,
                           AsignadorAprobador asignador, Clock clock) {
        this.acceso = acceso;
        this.solicitudes = solicitudes;
        this.reglas = reglas;
        this.asignador = asignador;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SolicitudDetalle ejecutar(SolicitudId id, Actor actor) {
        var solicitud = acceso.cargarDelSolicitante(id, actor);
        solicitud.enviar(reglas.para(solicitud.getTipo()), asignador, clock.instant());
        solicitudes.guardar(solicitud);
        return SolicitudDetalle.de(solicitud);
    }
}
