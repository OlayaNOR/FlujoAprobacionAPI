package com.nicolas.flujos.domain.flujo;

import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import java.math.BigDecimal;
import java.util.Objects;

public record PasoAprobacion(int orden, RolAprobador rolAprobador, BigDecimal umbralMinimo, int tiempoLimiteHoras) {

    public PasoAprobacion {
        Objects.requireNonNull(rolAprobador);
        if (orden < 1) {
            throw new IllegalArgumentException("El orden de los pasos empieza en 1");
        }
        if (tiempoLimiteHoras < 1) {
            throw new ReglaNegocioException("El tiempo límite debe ser de al menos una hora");
        }
        if (umbralMinimo != null && umbralMinimo.signum() <= 0) {
            throw new ReglaNegocioException("El umbral debe ser mayor que cero");
        }
    }

    /** Sin umbral el paso aplica siempre. Con umbral, desde ese valor en adelante. */
    public boolean aplicaA(BigDecimal magnitud) {
        return umbralMinimo == null || magnitud.compareTo(umbralMinimo) >= 0;
    }

    public PasoAprobacion conOrden(int nuevoOrden) {
        return new PasoAprobacion(nuevoOrden, rolAprobador, umbralMinimo, tiempoLimiteHoras);
    }
}
