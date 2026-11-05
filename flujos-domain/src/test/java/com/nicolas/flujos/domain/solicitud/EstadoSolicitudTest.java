package com.nicolas.flujos.domain.solicitud;

import static com.nicolas.flujos.domain.solicitud.EstadoSolicitud.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EstadoSolicitudTest {

    private static final Map<EstadoSolicitud, Set<EstadoSolicitud>> PERMITIDAS = Map.of(
        BORRADOR, EnumSet.of(PENDIENTE, CANCELADA),
        PENDIENTE, EnumSet.of(PENDIENTE, APROBADA, RECHAZADA, CANCELADA),
        APROBADA, EnumSet.of(CERRADA),
        RECHAZADA, EnumSet.noneOf(EstadoSolicitud.class),
        CANCELADA, EnumSet.noneOf(EstadoSolicitud.class),
        CERRADA, EnumSet.noneOf(EstadoSolicitud.class));

    @Test
    void cumpleLaTablaDeTransicionesCompleta() {
        for (var origen : EstadoSolicitud.values()) {
            for (var destino : EstadoSolicitud.values()) {
                assertThat(origen.puedeTransicionarA(destino))
                    .as("%s -> %s", origen, destino)
                    .isEqualTo(PERMITIDAS.get(origen).contains(destino));
            }
        }
    }
}
