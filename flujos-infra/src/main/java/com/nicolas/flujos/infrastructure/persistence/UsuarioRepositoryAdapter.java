package com.nicolas.flujos.infrastructure.persistence;

import com.nicolas.flujos.application.port.out.CuentaUsuario;
import com.nicolas.flujos.application.port.out.UsuarioRepository;
import com.nicolas.flujos.domain.flujo.RolAprobador;
import com.nicolas.flujos.domain.usuario.Rol;
import com.nicolas.flujos.domain.usuario.Usuario;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

@Repository
class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository jpa;

    UsuarioRepositoryAdapter(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Usuario> buscarPorId(UsuarioId id) {
        return jpa.findById(id.valor()).map(UsuarioRepositoryAdapter::aDominio);
    }

    @Override
    public Optional<CuentaUsuario> buscarCuentaPorEmail(String email) {
        return jpa.findByEmail(email).map(u -> new CuentaUsuario(aDominio(u), u.passwordHash));
    }

    @Override
    public Optional<UsuarioId> buscarAprobadorConCargo(RolAprobador cargo, UsuarioId excluido) {
        return jpa.buscarIdsConCargo(cargo, excluido.valor(), Rol.APROBADOR, Limit.of(1)).stream()
            .findFirst()
            .map(UsuarioId::new);
    }

    private static Usuario aDominio(UsuarioEntity u) {
        var jefe = u.jefeId == null ? null : new UsuarioId(u.jefeId);
        return new Usuario(new UsuarioId(u.id), u.email, u.nombre, u.roles, u.cargo, jefe, u.activo);
    }
}
