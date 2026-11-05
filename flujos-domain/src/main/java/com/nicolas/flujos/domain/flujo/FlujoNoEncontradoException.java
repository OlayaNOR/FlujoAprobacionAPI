package com.nicolas.flujos.domain.flujo;

public class FlujoNoEncontradoException extends RuntimeException {

    public FlujoNoEncontradoException() {
        super("Flujo no encontrado");
    }
}
