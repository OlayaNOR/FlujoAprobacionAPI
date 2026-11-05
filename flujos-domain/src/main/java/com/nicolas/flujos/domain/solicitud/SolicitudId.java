package com.nicolas.flujos.domain.solicitud;

import java.util.Objects;
import java.util.UUID;

public record SolicitudId(UUID valor) {

    public SolicitudId {
        Objects.requireNonNull(valor);
    }

    public static SolicitudId nuevo() {
        return new SolicitudId(UUID.randomUUID());
    }
}
