package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.in.CerrarSolicitudUseCase;
import com.nicolas.flujos.application.port.in.SolicitudDetalle;
import com.nicolas.flujos.application.port.out.AuditoriaRepository;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.auditoria.AccionAuditoria;
import com.nicolas.flujos.domain.auditoria.RegistroAuditoria;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import java.time.Clock;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CerrarSolicitudService implements CerrarSolicitudUseCase {

    private final AccesoSolicitudes acceso;
    private final SolicitudRepository solicitudes;
    private final AuditoriaRepository auditoria;
    private final Clock clock;

    CerrarSolicitudService(AccesoSolicitudes acceso, SolicitudRepository solicitudes, AuditoriaRepository auditoria,
                           Clock clock) {
        this.acceso = acceso;
        this.solicitudes = solicitudes;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SolicitudDetalle ejecutar(SolicitudId id, Actor actor) {
        var solicitud = acceso.cargarVisible(id, actor);
        var estadoAnterior = solicitud.getEstado();
        var ahora = clock.instant();

        solicitud.cerrar(ahora);

        solicitudes.guardar(solicitud);
        auditoria.registrar(RegistroAuditoria.de(solicitud, AccionAuditoria.CERRADA, actor.id(), estadoAnterior,
            Map.of(), ahora));
        return SolicitudDetalle.de(solicitud);
    }
}
