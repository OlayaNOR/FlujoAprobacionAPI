package com.nicolas.flujos.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import java.time.Duration;

/**
 * Una cubeta por IP o por email, en memoria. Una cubeta que pasa {@code periodo} sin usarse ya se llenó de nuevo,
 * así que descartarla no cambia nada y evita que el mapa crezca sin límite.
 */
class CubetasPorClave {

    private static final int MAXIMO_CLAVES = 100_000;

    private final Cache<String, Bucket> cubetas;
    private final long capacidad;
    private final Duration periodo;
    private final TimeMeter reloj;

    CubetasPorClave(long capacidad, Duration periodo, TimeMeter reloj) {
        this.capacidad = capacidad;
        this.periodo = periodo;
        this.reloj = reloj;
        this.cubetas = Caffeine.newBuilder().expireAfterAccess(periodo).maximumSize(MAXIMO_CLAVES).build();
    }

    Bucket de(String clave) {
        return cubetas.get(clave, c -> Bucket.builder()
            .addLimit(limite -> limite.capacity(capacidad).refillGreedy(capacidad, periodo))
            .withCustomTimePrecision(reloj)
            .build());
    }

    void olvidar(String clave) {
        cubetas.invalidate(clave);
    }

    /** Cuánto falta para que vuelva a haber un intento disponible. */
    static Duration espera(Bucket cubeta) {
        return Duration.ofNanos(cubeta.estimateAbilityToConsume(1).getNanosToWaitForRefill());
    }
}
