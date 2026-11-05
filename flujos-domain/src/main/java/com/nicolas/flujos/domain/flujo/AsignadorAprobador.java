package com.nicolas.flujos.domain.flujo;

import com.nicolas.flujos.domain.usuario.UsuarioId;

@FunctionalInterface
public interface AsignadorAprobador {

    UsuarioId aprobadorPara(RolAprobador rol, UsuarioId solicitanteId);
}
