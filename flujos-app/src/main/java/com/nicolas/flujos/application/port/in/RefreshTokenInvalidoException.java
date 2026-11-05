package com.nicolas.flujos.application.port.in;

public class RefreshTokenInvalidoException extends RuntimeException {

    public RefreshTokenInvalidoException() {
        super("El refresh token no es válido o expiró");
    }
}
