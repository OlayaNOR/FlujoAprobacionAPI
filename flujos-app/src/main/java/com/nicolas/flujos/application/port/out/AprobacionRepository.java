package com.nicolas.flujos.application.port.out;

import com.nicolas.flujos.domain.solicitud.Decision;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import com.nicolas.flujos.domain.usuario.UsuarioId;

public interface AprobacionRepository {

    void registrar(Decision decision);

    boolean participo(SolicitudId solicitudId, UsuarioId usuarioId);
}
