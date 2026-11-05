package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.in.EnviarSolicitudUseCase;
import com.nicolas.flujos.application.port.in.SolicitudDetalle;
import com.nicolas.flujos.application.port.out.AuditoriaRepository;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.auditoria.AccionAuditoria;
import com.nicolas.flujos.domain.auditoria.RegistroAuditoria;
import com.nicolas.flujos.domain.regla.AsignadorAprobador;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import java.time.Clock;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class EnviarSolicitudService implements EnviarSolicitudUseCase {

    private final AccesoSolicitudes acceso;
    private final SolicitudRepository solicitudes;
    private final ReglasAprobacionRegistry reglas;
    private final AsignadorAprobador asignador;
    private final AuditoriaRepository auditoria;
    private final Clock clock;

    EnviarSolicitudService(AccesoSolicitudes acceso, SolicitudRepository solicitudes, ReglasAprobacionRegistry reglas,
                           AsignadorAprobador asignador, AuditoriaRepository auditoria, Clock clock) {
        this.acceso = acceso;
        this.solicitudes = solicitudes;
        this.reglas = reglas;
        this.asignador = asignador;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SolicitudDetalle ejecutar(SolicitudId id, Actor actor) {
        var solicitud = acceso.cargarDelSolicitante(id, actor);
        var estadoAnterior = solicitud.getEstado();
        var ahora = clock.instant();

        solicitud.enviar(reglas.para(solicitud.getTipo()), asignador, ahora);

        solicitudes.guardar(solicitud);
        auditoria.registrar(solicitud.getEstado() == EstadoSolicitud.APROBADA
            ? RegistroAuditoria.de(solicitud, AccionAuditoria.APROBADA, null, estadoAnterior,
                Map.of("automatica", true, "enviadaPor", actor.id().valor().toString()), ahora)
            : RegistroAuditoria.de(solicitud, AccionAuditoria.ENVIADA, actor.id(), estadoAnterior,
                Map.of("paso", solicitud.getPasoActual()), ahora));
        return SolicitudDetalle.de(solicitud);
    }
}
