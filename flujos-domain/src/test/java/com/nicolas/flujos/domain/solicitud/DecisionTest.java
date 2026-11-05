package com.nicolas.flujos.domain.solicitud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class DecisionTest {

    @Test
    void aprobarNoExigeComentario() {
        var decision = decision(ResultadoDecision.APROBADA, null);

        assertThat(decision.comentario()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rechazarSinMotivoNoEsValido(String comentario) {
        assertThatThrownBy(() -> decision(ResultadoDecision.RECHAZADA, comentario))
            .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void unComentarioDemasiadoLargoNoEsValido() {
        var largo = "x".repeat(501);

        assertThatThrownBy(() -> decision(ResultadoDecision.APROBADA, largo))
            .isInstanceOf(ReglaNegocioException.class);
    }

    private static Decision decision(ResultadoDecision resultado, String comentario) {
        return new Decision(SolicitudId.nuevo(), 1, Solicitudes.JEFE, resultado, comentario, Solicitudes.AHORA);
    }
}
