package com.nicolas.flujos.domain.flujo;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record PasoAprobacion(int orden, RolAprobador rolAprobador, BigDecimal umbralMinimo, int tiempoLimiteHoras) {

    public PasoAprobacion {
        Objects.requireNonNull(rolAprobador);
        if (orden < 1) {
            throw new IllegalArgumentException("El orden de los pasos empieza en 1");
        }
        if (tiempoLimiteHoras < 1) {
            throw new IllegalArgumentException("El tiempo límite debe ser de al menos una hora");
        }
    }

    public static List<PasoAprobacion> secuencia(Duration tiempoLimite, RolAprobador... roles) {
        var pasos = new ArrayList<PasoAprobacion>();
        for (int i = 0; i < roles.length; i++) {
            pasos.add(new PasoAprobacion(i + 1, roles[i], null, (int) tiempoLimite.toHours()));
        }
        return List.copyOf(pasos);
    }
}
