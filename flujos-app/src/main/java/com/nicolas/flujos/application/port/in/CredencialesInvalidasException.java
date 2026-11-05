package com.nicolas.flujos.application.port.in;

public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Email o contraseña incorrectos");
    }
}
