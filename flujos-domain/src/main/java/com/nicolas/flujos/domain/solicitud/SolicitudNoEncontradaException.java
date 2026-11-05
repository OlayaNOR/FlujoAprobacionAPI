package com.nicolas.flujos.domain.solicitud;

public class SolicitudNoEncontradaException extends RuntimeException {

    public SolicitudNoEncontradaException(SolicitudId id) {
        super("No existe la solicitud " + id.valor());
    }
}
