package com.nicolas.flujos.application.port.in;

import com.nicolas.flujos.domain.usuario.Rol;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.util.Set;

public record Actor(UsuarioId id, Set<Rol> roles) {

    public Actor {
        roles = Set.copyOf(roles);
    }

    public boolean esAdmin() {
        return roles.contains(Rol.ADMIN);
    }
}
