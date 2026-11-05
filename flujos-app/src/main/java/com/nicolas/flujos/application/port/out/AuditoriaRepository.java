package com.nicolas.flujos.application.port.out;

import com.nicolas.flujos.domain.auditoria.RegistroAuditoria;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import java.util.List;

public interface AuditoriaRepository {

    void registrar(RegistroAuditoria registro);

    List<RegistroAuditoria> listarPorSolicitud(SolicitudId id);
}
