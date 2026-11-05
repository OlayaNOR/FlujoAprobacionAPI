package com.nicolas.flujos.domain.regla;

import static com.nicolas.flujos.domain.flujo.RolAprobador.DIRECTOR;
import static com.nicolas.flujos.domain.flujo.RolAprobador.GERENTE;
import static com.nicolas.flujos.domain.flujo.RolAprobador.JEFE;
import static com.nicolas.flujos.domain.flujo.RolAprobador.RRHH;
import static com.nicolas.flujos.domain.solicitud.Solicitudes.AHORA;
import static com.nicolas.flujos.domain.solicitud.Solicitudes.ASIGNADOR;
import static com.nicolas.flujos.domain.solicitud.Solicitudes.HOY;
import static com.nicolas.flujos.domain.solicitud.Solicitudes.LIMITE;
import static com.nicolas.flujos.domain.solicitud.Solicitudes.SOLICITANTE;
import static com.nicolas.flujos.domain.solicitud.Solicitudes.USD;
import static org.assertj.core.api.Assertions.assertThat;

import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.flujo.RolAprobador;
import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.shared.Periodo;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.Solicitudes;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReglasAprobacionTest {

    private final ReglaVacaciones vacaciones = new ReglaVacaciones(LIMITE);
    private final ReglaCompra compra = new ReglaCompra(LIMITE);
    private final ReglaGasto gasto = new ReglaGasto(LIMITE);

    @Test
    void vacacionesDeTresDiasSoloRequierenAlJefe() {
        assertThat(roles(vacaciones.pasosRequeridos(vacacionesDe(3)))).containsExactly(JEFE);
    }

    @Test
    void vacacionesDeCuatroDiasRequierenJefeYRrhh() {
        assertThat(roles(vacaciones.pasosRequeridos(vacacionesDe(4)))).containsExactly(JEFE, RRHH);
    }

    @ParameterizedTest
    @CsvSource({"999.99, 1", "1000, 2", "10000, 2", "10000.01, 3"})
    void compraRequierePasosSegunElMonto(String monto, int pasosEsperados) {
        assertThat(compra.pasosRequeridos(Solicitudes.compra(monto))).hasSize(pasosEsperados);
    }

    @Test
    void compraGrandePasaPorJefeGerenteYDirectorEnEseOrden() {
        var pasos = compra.pasosRequeridos(Solicitudes.compra("20000"));

        assertThat(roles(pasos)).containsExactly(JEFE, GERENTE, DIRECTOR);
        assertThat(pasos).extracting(PasoAprobacion::orden).containsExactly(1, 2, 3);
    }

    @Test
    void gastoMenorA200NoRequierePasos() {
        assertThat(gasto.pasosRequeridos(gastoDe("199.99"))).isEmpty();
    }

    @Test
    void gastoDe200RequiereAlJefe() {
        assertThat(roles(gasto.pasosRequeridos(gastoDe("200")))).containsExactly(JEFE);
    }

    @Test
    void gastoPequenoEnviadoQuedaAprobadoSinPasarPorPendiente() {
        var solicitud = gastoDe("50");

        solicitud.enviar(gasto, ASIGNADOR, AHORA);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.APROBADA);
    }

    @Test
    void compraDeTresPasosRecorreTodoElFlujoHastaAprobada() {
        var solicitud = Solicitudes.compra("20000");
        solicitud.enviar(compra, ASIGNADOR, AHORA);

        solicitud.aprobar(solicitud.getAprobadorActualId(), compra, ASIGNADOR, AHORA);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        solicitud.aprobar(solicitud.getAprobadorActualId(), compra, ASIGNADOR, AHORA);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        solicitud.aprobar(solicitud.getAprobadorActualId(), compra, ASIGNADOR, AHORA);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.APROBADA);
    }

    private static List<RolAprobador> roles(List<PasoAprobacion> pasos) {
        return pasos.stream().map(PasoAprobacion::rolAprobador).toList();
    }

    private static Solicitud vacacionesDe(int dias) {
        var periodo = new Periodo(HOY, HOY.plusDays(dias - 1L));
        return Solicitud.nuevasVacaciones(SOLICITANTE, periodo, null, HOY, AHORA);
    }

    private static Solicitud gastoDe(String monto) {
        return Solicitud.nuevoGasto(SOLICITANTE, Money.de(monto, "USD"), "Taxi", USD, AHORA);
    }
}
