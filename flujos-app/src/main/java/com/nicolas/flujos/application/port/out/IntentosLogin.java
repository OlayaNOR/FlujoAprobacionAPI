package com.nicolas.flujos.application.port.out;

import java.time.Duration;
import java.util.Optional;

/** Cuenta los logins fallidos por email, exista o no la cuenta, para que el bloqueo no delate cuáles existen. */
public interface IntentosLogin {

    /** Cuánto falta para poder intentar de nuevo, o vacío si no está bloqueado. */
    Optional<Duration> bloqueo(String email);

    void registrarFallo(String email);

    void limpiar(String email);
}
