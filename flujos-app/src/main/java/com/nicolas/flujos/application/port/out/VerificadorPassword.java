package com.nicolas.flujos.application.port.out;

public interface VerificadorPassword {

    /** Con {@code hash} null devuelve false, pero tarda lo mismo que una verificación real. */
    boolean coincide(String password, String hash);
}
