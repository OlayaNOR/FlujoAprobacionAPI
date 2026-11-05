package com.nicolas.flujos.domain.usuario;

import java.util.Objects;
import java.util.UUID;

public record UsuarioId(UUID valor) {

    public UsuarioId {
        Objects.requireNonNull(valor);
    }

    public static UsuarioId nuevo() {
        return new UsuarioId(UUID.randomUUID());
    }
}
