package com.nicolas.flujos.application.port.in;

import com.nicolas.flujos.domain.auditoria.RegistroAuditoria;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import java.util.List;

public interface ConsultarAuditoriaUseCase {

    List<RegistroAuditoria> ejecutar(SolicitudId id, Actor actor);
}
