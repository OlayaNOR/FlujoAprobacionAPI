package com.nicolas.flujos.application.port.in;

import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.time.Instant;
import java.util.Set;

public record FiltroSolicitud(Set<EstadoSolicitud> estados, TipoSolicitud tipo, Instant desde, Instant hasta) {

    public FiltroSolicitud {
        estados = estados == null ? Set.of() : Set.copyOf(estados);
    }
}
