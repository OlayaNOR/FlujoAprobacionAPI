package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.out.UsuarioRepository;
import com.nicolas.flujos.domain.flujo.RolAprobador;
import com.nicolas.flujos.domain.flujo.AsignadorAprobador;
import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import com.nicolas.flujos.domain.usuario.Usuario;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import org.springframework.stereotype.Service;

@Service
class AsignadorPorJerarquia implements AsignadorAprobador {

    private final UsuarioRepository usuarios;

    AsignadorPorJerarquia(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UsuarioId aprobadorPara(RolAprobador rol, UsuarioId solicitanteId) {
        if (rol == RolAprobador.JEFE) {
            return jefeDirecto(solicitanteId);
        }
        return usuarios.buscarAprobadorConCargo(rol, solicitanteId)
            .orElseThrow(() -> new ReglaNegocioException("No hay un aprobador disponible con el cargo " + rol));
    }

    private UsuarioId jefeDirecto(UsuarioId solicitanteId) {
        return usuarios.buscarPorId(solicitanteId)
            .map(Usuario::jefeId)
            .flatMap(usuarios::buscarPorId)
            .filter(Usuario::puedeAprobar)
            .map(Usuario::id)
            .orElseThrow(() -> new ReglaNegocioException("El solicitante no tiene un jefe directo que pueda aprobar"));
    }
}
