package com.nicolas.flujos.domain.solicitud;

public class SolicitudNoEditableException extends RuntimeException {

    private final EstadoSolicitud estadoActual;

    public SolicitudNoEditableException(EstadoSolicitud estadoActual) {
        super("La solicitud está en %s y solo se puede editar en BORRADOR.".formatted(estadoActual));
        this.estadoActual = estadoActual;
    }

    public EstadoSolicitud getEstadoActual() {
        return estadoActual;
    }
}
