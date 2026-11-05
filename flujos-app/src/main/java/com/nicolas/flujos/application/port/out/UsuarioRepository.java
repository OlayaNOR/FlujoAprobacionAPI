package com.nicolas.flujos.application.port.out;

import com.nicolas.flujos.domain.flujo.RolAprobador;
import com.nicolas.flujos.domain.usuario.Usuario;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.util.Optional;

public interface UsuarioRepository {

    Optional<Usuario> buscarPorId(UsuarioId id);

    Optional<CuentaUsuario> buscarCuentaPorEmail(String email);

    /** Si varios usuarios tienen el cargo, siempre devuelve al más antiguo. */
    Optional<UsuarioId> buscarAprobadorConCargo(RolAprobador cargo, UsuarioId excluido);
}
