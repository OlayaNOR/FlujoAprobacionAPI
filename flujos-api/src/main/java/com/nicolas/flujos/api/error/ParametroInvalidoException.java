package com.nicolas.flujos.api.error;

public class ParametroInvalidoException extends RuntimeException {

    public ParametroInvalidoException(String mensaje) {
        super(mensaje);
    }
}
