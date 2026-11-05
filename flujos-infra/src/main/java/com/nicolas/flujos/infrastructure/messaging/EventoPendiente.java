package com.nicolas.flujos.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

public record EventoPendiente(long id, UUID agregadoId, String tipo, String payload, Instant creadoEn) {
}
