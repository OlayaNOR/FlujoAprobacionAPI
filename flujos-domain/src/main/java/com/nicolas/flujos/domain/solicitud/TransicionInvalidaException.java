package com.nicolas.flujos.domain.solicitud;

public class TransicionInvalidaException extends RuntimeException {

    private final EstadoSolicitud estadoActual;

    public TransicionInvalidaException(EstadoSolicitud estadoActual, EstadoSolicitud destino) {
        super("La solicitud está en %s y no puede pasar a %s.".formatted(estadoActual, destino));
        this.estadoActual = estadoActual;
    }

    public EstadoSolicitud getEstadoActual() {
        return estadoActual;
    }
}
