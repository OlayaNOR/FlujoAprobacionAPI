package com.nicolas.flujos.domain.flujo;

import java.util.Objects;
import java.util.UUID;

public record FlujoId(UUID valor) {

    public FlujoId {
        Objects.requireNonNull(valor);
    }
}
