package com.nicolas.flujos.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyTest {

    @Test
    void redondeaADosDecimalesConHalfEven() {
        assertThat(Money.de("10.125", "USD").valor()).isEqualByComparingTo("10.12");
        assertThat(Money.de("10.135", "USD").valor()).isEqualByComparingTo("10.14");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "0.001"})
    void rechazaMontosQueNoSonPositivos(String valor) {
        assertThatThrownBy(() -> Money.de(valor, "USD"))
            .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void dosMontosIgualesSonEquivalentesSinImportarLaEscala() {
        assertThat(Money.de("100", "USD")).isEqualTo(Money.de("100.00", "USD"));
    }

    @Test
    void sumaMontosDeLaMismaMoneda() {
        assertThat(Money.de("10.50", "USD").sumar(Money.de("0.25", "USD")))
            .isEqualTo(Money.de("10.75", "USD"));
    }

    @Test
    void comparaMontosDeLaMismaMoneda() {
        var menor = Money.de("10", "USD");
        var mayor = Money.de("20", "USD");

        assertThat(menor.esMenorQue(mayor)).isTrue();
        assertThat(mayor.esMayorQue(menor)).isTrue();
        assertThat(menor.esMenorQue(menor)).isFalse();
    }

    @Test
    void noOperaEntreMonedasDistintas() {
        var dolares = Money.de("10", "USD");
        var pesos = Money.de("10", "COP");

        assertThatThrownBy(() -> dolares.sumar(pesos)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> dolares.esMayorQue(pesos)).isInstanceOf(ReglaNegocioException.class);
    }
}
