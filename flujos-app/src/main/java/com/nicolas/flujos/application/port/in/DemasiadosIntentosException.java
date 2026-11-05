package com.nicolas.flujos.application.port.in;

import java.time.Duration;

public class DemasiadosIntentosException extends RuntimeException {

    private final Duration reintentarEn;

    public DemasiadosIntentosException(Duration reintentarEn) {
        super("Demasiados intentos fallidos con este email. Espera antes de volver a intentar.");
        this.reintentarEn = reintentarEn;
    }

    public Duration getReintentarEn() {
        return reintentarEn;
    }
}
