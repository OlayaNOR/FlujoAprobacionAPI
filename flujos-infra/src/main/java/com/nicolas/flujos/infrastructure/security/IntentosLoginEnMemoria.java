package com.nicolas.flujos.infrastructure.security;

import com.nicolas.flujos.application.port.out.IntentosLogin;
import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// En memoria: con varias instancias cada una cuenta por su lado (ADR-008).
@Component
class IntentosLoginEnMemoria implements IntentosLogin {

    private final CubetasPorClave cubetas;

    @Autowired
    IntentosLoginEnMemoria(LimiteLoginProperties limites) {
        this(limites.fallosPorHoraEmail(), TimeMeter.SYSTEM_NANOTIME);
    }

    IntentosLoginEnMemoria(int fallosPorHora, TimeMeter reloj) {
        this.cubetas = new CubetasPorClave(fallosPorHora, Duration.ofHours(1), reloj);
    }

    @Override
    public Optional<Duration> bloqueo(String email) {
        var cubeta = cubetas.de(email);
        return cubeta.getAvailableTokens() > 0 ? Optional.empty() : Optional.of(CubetasPorClave.espera(cubeta));
    }

    @Override
    public void registrarFallo(String email) {
        cubetas.de(email).tryConsume(1);
    }

    @Override
    public void limpiar(String email) {
        cubetas.olvidar(email);
    }
}
