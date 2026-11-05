package com.nicolas.flujos.domain.solicitud;

public enum EstadoSolicitud {
    BORRADOR, PENDIENTE, APROBADA, RECHAZADA, CANCELADA, CERRADA;

    public boolean puedeTransicionarA(EstadoSolicitud destino) {
        return switch (this) {
            case BORRADOR -> destino == PENDIENTE || destino == CANCELADA;
            case PENDIENTE -> destino == PENDIENTE || destino == APROBADA
                || destino == RECHAZADA || destino == CANCELADA;
            case APROBADA -> destino == CERRADA;
            case RECHAZADA, CANCELADA, CERRADA -> false;
        };
    }
}
