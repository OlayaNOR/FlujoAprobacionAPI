package com.nicolas.flujos.domain.shared;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record Periodo(LocalDate inicio, LocalDate fin) {

    public Periodo {
        Objects.requireNonNull(inicio);
        Objects.requireNonNull(fin);
        if (fin.isBefore(inicio)) {
            throw new ReglaNegocioException("La fecha de fin no puede ser anterior a la de inicio");
        }
    }

    public long dias() {
        return ChronoUnit.DAYS.between(inicio, fin) + 1;
    }
}
