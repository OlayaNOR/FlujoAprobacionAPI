package com.nicolas.flujos.domain.flujo;

import static com.nicolas.flujos.domain.flujo.RolAprobador.DIRECTOR;
import static com.nicolas.flujos.domain.flujo.RolAprobador.GERENTE;
import static com.nicolas.flujos.domain.flujo.RolAprobador.JEFE;
import static com.nicolas.flujos.domain.flujo.RolAprobador.RRHH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FlujoTest {

    private static final int HORAS = 48;

    @ParameterizedTest
    @CsvSource({"999.99, 1", "1000, 2", "10000, 2", "10000.01, 3"})
    void lasComprasRequierenPasosSegunElMonto(String monto, int esperados) {
        assertThat(compras().pasosPara(new BigDecimal(monto))).hasSize(esperados);
    }

    @Test
    void lasVacacionesCuentanDias() {
        var flujo = flujo(TipoSolicitud.VACACIONES, paso(1, JEFE, null), paso(2, RRHH, "4"));

        assertThat(roles(flujo.pasosPara(BigDecimal.valueOf(3)))).containsExactly(JEFE);
        assertThat(roles(flujo.pasosPara(BigDecimal.valueOf(4)))).containsExactly(JEFE, RRHH);
    }

    @Test
    void siNingunPasoAplicaNoRequiereAprobadores() {
        var gastos = flujo(TipoSolicitud.GASTO, paso(1, JEFE, "200"));

        assertThat(gastos.pasosPara(new BigDecimal("199.99"))).isEmpty();
        assertThat(roles(gastos.pasosPara(new BigDecimal("200")))).containsExactly(JEFE);
    }

    @Test
    void losPasosQueAplicanSeNumeranSinHuecos() {
        var flujo = flujo(TipoSolicitud.COMPRA, paso(1, JEFE, "1000"), paso(2, GERENTE, null));

        var pasos = flujo.pasosPara(new BigDecimal("500"));

        assertThat(roles(pasos)).containsExactly(GERENTE);
        assertThat(pasos.get(0).orden()).isEqualTo(1);
    }

    @Test
    void unFlujoDesactivadoNoEntregaPasos() {
        var flujo = Flujo.reconstituir(new FlujoId(UUID.randomUUID()), TipoSolicitud.COMPRA, false,
            List.of(paso(1, JEFE, null)), 1);

        assertThatThrownBy(() -> flujo.pasosPara(BigDecimal.TEN)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void reemplazarLosPasosSubeLaVersion() {
        var flujo = compras();

        flujo.reemplazarPasos(List.of(paso(1, DIRECTOR, null)));

        assertThat(roles(flujo.getPasos())).containsExactly(DIRECTOR);
        assertThat(flujo.getVersion()).isEqualTo(2);
    }

    @Test
    void reemplazarConPasosInvalidosNoCambiaNada() {
        var flujo = compras();

        assertThatThrownBy(() -> flujo.reemplazarPasos(List.of())).isInstanceOf(ReglaNegocioException.class);

        assertThat(flujo.getPasos()).hasSize(3);
        assertThat(flujo.getVersion()).isEqualTo(1);
    }

    @Test
    void unRolNoSePuedeRepetir() {
        var pasos = List.of(paso(1, JEFE, null), paso(2, JEFE, "1000"));

        assertThatThrownBy(() -> compras().reemplazarPasos(pasos))
            .isInstanceOf(ReglaNegocioException.class)
            .hasMessageContaining("JEFE");
    }

    @Test
    void losPasosDebenIrEnOrdenYSinHuecos() {
        var pasos = List.of(paso(1, JEFE, null), paso(3, GERENTE, null));

        assertThatThrownBy(() -> compras().reemplazarPasos(pasos)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void enVacacionesElUmbralEsUnNumeroEnteroDeDias() {
        var flujo = flujo(TipoSolicitud.VACACIONES, paso(1, JEFE, null));

        assertThatThrownBy(() -> flujo.reemplazarPasos(List.of(paso(1, JEFE, "3.5"))))
            .isInstanceOf(ReglaNegocioException.class);
        flujo.reemplazarPasos(List.of(paso(1, JEFE, "4.00")));
    }

    @Test
    void elUmbralYElTiempoLimiteDebenSerPositivos() {
        assertThatThrownBy(() -> paso(1, JEFE, "0")).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> new PasoAprobacion(1, JEFE, null, 0)).isInstanceOf(ReglaNegocioException.class);
    }

    private static Flujo compras() {
        return flujo(TipoSolicitud.COMPRA, paso(1, JEFE, null), paso(2, GERENTE, "1000"), paso(3, DIRECTOR, "10000.01"));
    }

    private static Flujo flujo(TipoSolicitud tipo, PasoAprobacion... pasos) {
        return Flujo.reconstituir(new FlujoId(UUID.randomUUID()), tipo, true, List.of(pasos), 1);
    }

    private static PasoAprobacion paso(int orden, RolAprobador rol, String umbral) {
        return new PasoAprobacion(orden, rol, umbral == null ? null : new BigDecimal(umbral), HORAS);
    }

    private static List<RolAprobador> roles(List<PasoAprobacion> pasos) {
        return pasos.stream().map(PasoAprobacion::rolAprobador).toList();
    }
}
