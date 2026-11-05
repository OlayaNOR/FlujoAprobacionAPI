package com.nicolas.flujos.domain.regla;

import com.nicolas.flujos.domain.flujo.RolAprobador;
import com.nicolas.flujos.domain.usuario.UsuarioId;

@FunctionalInterface
public interface AsignadorAprobador {

    UsuarioId aprobadorPara(RolAprobador rol, UsuarioId solicitanteId);
}
