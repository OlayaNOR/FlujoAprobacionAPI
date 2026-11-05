package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.in.ConsultarAuditoriaUseCase;
import com.nicolas.flujos.application.port.out.AuditoriaRepository;
import com.nicolas.flujos.domain.auditoria.RegistroAuditoria;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ConsultarAuditoriaService implements ConsultarAuditoriaUseCase {

    private final AccesoSolicitudes acceso;
    private final AuditoriaRepository auditoria;

    ConsultarAuditoriaService(AccesoSolicitudes acceso, AuditoriaRepository auditoria) {
        this.acceso = acceso;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroAuditoria> ejecutar(SolicitudId id, Actor actor) {
        acceso.cargarVisible(id, actor);
        return auditoria.listarPorSolicitud(id);
    }
}
