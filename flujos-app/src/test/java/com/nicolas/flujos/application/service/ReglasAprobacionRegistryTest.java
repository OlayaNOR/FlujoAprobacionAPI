package com.nicolas.flujos.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nicolas.flujos.domain.regla.ReglaCompra;
import com.nicolas.flujos.domain.regla.ReglaGasto;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReglasAprobacionRegistryTest {

    private static final Duration LIMITE = Duration.ofHours(48);

    @Test
    void resuelveLaReglaPorTipo() {
        var compra = new ReglaCompra(LIMITE);
        var registro = new ReglasAprobacionRegistry(List.of(compra, new ReglaGasto(LIMITE)));

        assertThat(registro.para(TipoSolicitud.COMPRA)).isSameAs(compra);
    }

    @Test
    void fallaSiUnTipoNoTieneRegla() {
        var registro = new ReglasAprobacionRegistry(List.of(new ReglaCompra(LIMITE)));

        assertThatThrownBy(() -> registro.para(TipoSolicitud.GASTO)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dosReglasParaElMismoTipoImpidenArrancar() {
        var repetidas = List.of(new ReglaCompra(LIMITE), new ReglaCompra(LIMITE));

        assertThatThrownBy(() -> new ReglasAprobacionRegistry(repetidas))
            .isInstanceOf(IllegalStateException.class);
    }
}
