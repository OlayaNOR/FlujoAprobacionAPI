package com.nicolas.flujos.application.port.in;

import com.nicolas.flujos.domain.solicitud.SolicitudId;
import java.util.List;

public record ResultadoEscalamiento(int escaladas, List<Fallo> fallidas) {

    public record Fallo(SolicitudId solicitudId, String motivo) {
    }
}
