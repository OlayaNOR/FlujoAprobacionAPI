package com.nicolas.flujos.domain.usuario;

import com.nicolas.flujos.domain.flujo.RolAprobador;
import java.util.Objects;
import java.util.Set;

public record Usuario(UsuarioId id, String email, String nombre, Set<Rol> roles,
                      RolAprobador cargo, UsuarioId jefeId, boolean activo) {

    public Usuario {
        Objects.requireNonNull(id);
        Objects.requireNonNull(email);
        roles = Set.copyOf(roles);
    }

    public boolean puedeAprobar() {
        return activo && roles.contains(Rol.APROBADOR);
    }
}
