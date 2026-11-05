package com.nicolas.flujos.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PeriodoTest {

    private static final LocalDate LUNES = LocalDate.parse("2026-11-09");

    @Test
    void cuentaLosDiasIncluyendoInicioYFin() {
        assertThat(new Periodo(LUNES, LUNES.plusDays(4)).dias()).isEqualTo(5);
    }

    @Test
    void unPeriodoDeUnSoloDiaCuentaComoUno() {
        assertThat(new Periodo(LUNES, LUNES).dias()).isEqualTo(1);
    }

    @Test
    void rechazaFinAnteriorAInicio() {
        assertThatThrownBy(() -> new Periodo(LUNES, LUNES.minusDays(1)))
            .isInstanceOf(ReglaNegocioException.class);
    }
}
