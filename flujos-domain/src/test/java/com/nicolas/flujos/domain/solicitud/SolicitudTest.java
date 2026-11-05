package com.nicolas.flujos.domain.solicitud;

import static com.nicolas.flujos.domain.flujo.RolAprobador.DIRECTOR;
import static com.nicolas.flujos.domain.flujo.RolAprobador.GERENTE;
import static com.nicolas.flujos.domain.flujo.RolAprobador.JEFE;
import static com.nicolas.flujos.domain.flujo.RolAprobador.RRHH;
import static com.nicolas.flujos.domain.solicitud.Solicitudes.AHORA;
import static com.nicolas.flujos.domain.solicitud.Solicitudes.ASIGNADOR;
import static com.nicolas.flujos.domain.solicitud.Solicitudes.HOY;
import static com.nicolas.flujos.domain.solicitud.Solicitudes.LIMITE;
import static com.nicolas.flujos.domain.solicitud.Solicitudes.OTRO;
import static com.nicolas.flujos.domain.solicitud.Solicitudes.SOLICITANTE;
import static com.nicolas.flujos.domain.solicitud.Solicitudes.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nicolas.flujos.domain.flujo.Flujo;
import com.nicolas.flujos.domain.flujo.FlujoId;
import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.shared.Periodo;
import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import com.nicolas.flujos.domain.solicitud.EventoDominio.SolicitudAprobada;
import com.nicolas.flujos.domain.solicitud.EventoDominio.SolicitudEnviada;
import com.nicolas.flujos.domain.solicitud.EventoDominio.SolicitudEscalada;
import com.nicolas.flujos.domain.solicitud.EventoDominio.SolicitudRechazada;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SolicitudTest {

    private static final Flujo UN_PASO = Solicitudes.flujoCon(JEFE);
    private static final Flujo DOS_PASOS = Solicitudes.flujoCon(JEFE, GERENTE);
    // Solicitudes.enBorrador() es de 500: no alcanza el umbral de ningún paso
    private static final Flujo SIN_PASOS = Solicitudes.flujoCon(
        List.of(new PasoAprobacion(1, JEFE, new BigDecimal("1000"), 48)));
    private static final Instant VENCIDA = AHORA.plus(LIMITE).plus(Duration.ofMinutes(1));

    @Nested
    class Creacion {

        @Test
        void empiezaEnBorradorYSinPaso() {
            var solicitud = Solicitudes.enBorrador();

            assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
            assertThat(solicitud.getPasoActual()).isZero();
            assertThat(solicitud.getAprobadorActualId()).isNull();
            assertThat(solicitud.pullEventos()).isEmpty();
        }

        @Test
        void compraSinMontoSeRechaza() {
            assertThatThrownBy(() -> Solicitud.nuevaCompra(SOLICITANTE, null, "x", USD, AHORA))
                .isInstanceOf(ReglaNegocioException.class);
        }

        @Test
        void montoEnOtraMonedaSeRechaza() {
            var pesos = Money.de("100", "COP");

            assertThatThrownBy(() -> Solicitud.nuevoGasto(SOLICITANTE, pesos, "x", USD, AHORA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("USD");
        }

        @Test
        void vacacionesEnElPasadoSeRechazan() {
            var periodo = new Periodo(HOY.minusDays(1), HOY.plusDays(2));

            assertThatThrownBy(() -> Solicitud.nuevasVacaciones(SOLICITANTE, periodo, null, HOY, AHORA))
                .isInstanceOf(ReglaNegocioException.class);
        }

        @Test
        void vacacionesQueEmpiezanHoySonValidas() {
            var solicitud = Solicitud.nuevasVacaciones(SOLICITANTE, new Periodo(HOY, HOY), null, HOY, AHORA);

            assertThat(solicitud.getTipo()).isEqualTo(TipoSolicitud.VACACIONES);
            assertThat(solicitud.getMonto()).isNull();
        }

        @Test
        void motivoDeMasDe500CaracteresSeRechaza() {
            var largo = "x".repeat(501);

            assertThatThrownBy(() -> Solicitud.nuevaCompra(SOLICITANTE, Money.de("1", "USD"), largo, USD, AHORA))
                .isInstanceOf(ReglaNegocioException.class);
        }
    }

    @Nested
    class Edicion {

        @Test
        void enBorradorPermiteCambiarMontoYMotivo() {
            var solicitud = Solicitudes.enBorrador();
            var despues = AHORA.plusSeconds(60);

            solicitud.editarMonto(Money.de("900", "USD"), "Otro motivo", USD, despues);

            assertThat(solicitud.getMonto()).isEqualTo(Money.de("900", "USD"));
            assertThat(solicitud.getMotivo()).isEqualTo("Otro motivo");
            assertThat(solicitud.getActualizadaEn()).isEqualTo(despues);
        }

        @Test
        void despuesDeEnviarNoSePuedeEditar() {
            var solicitud = Solicitudes.pendiente(UN_PASO);

            assertThatThrownBy(() -> solicitud.editarMonto(Money.de("900", "USD"), null, USD, AHORA))
                .isInstanceOf(SolicitudNoEditableException.class);
        }

        @Test
        void unaCompraNoAceptaPeriodo() {
            var solicitud = Solicitudes.enBorrador();

            assertThatThrownBy(() -> solicitud.editarPeriodo(new Periodo(HOY, HOY), null, HOY, AHORA))
                .isInstanceOf(ReglaNegocioException.class);
        }

        @Test
        void unasVacacionesNoAceptanMonto() {
            var solicitud = Solicitud.nuevasVacaciones(SOLICITANTE, new Periodo(HOY, HOY), null, HOY, AHORA);

            assertThatThrownBy(() -> solicitud.editarMonto(Money.de("1", "USD"), null, USD, AHORA))
                .isInstanceOf(ReglaNegocioException.class);
        }
    }

    @Nested
    class Envio {

        @Test
        void pasaAPendienteEnElPrimerPasoConAprobadorYFechaLimite() {
            var solicitud = Solicitudes.enBorrador();

            solicitud.enviar(DOS_PASOS, ASIGNADOR, AHORA);

            assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
            assertThat(solicitud.getPasoActual()).isEqualTo(1);
            assertThat(solicitud.getAprobadorActualId()).isEqualTo(Solicitudes.JEFE);
            assertThat(solicitud.getFechaLimitePaso()).isEqualTo(AHORA.plus(LIMITE));
            assertThat(solicitud.pullEventos()).containsExactly(
                new SolicitudEnviada(solicitud.getId(), Solicitudes.JEFE, AHORA));
        }

        @Test
        void sinPasosSeApruebaDirecto() {
            var solicitud = Solicitudes.enBorrador();

            solicitud.enviar(SIN_PASOS, ASIGNADOR, AHORA);

            assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.APROBADA);
            assertThat(solicitud.getFechaLimitePaso()).isNull();
            assertThat(solicitud.pullEventos()).containsExactly(new SolicitudAprobada(solicitud.getId(), AHORA));
        }

        @Test
        void noPermiteEnviarUnaSolicitudCancelada() {
            var solicitud = Solicitudes.enBorrador();
            solicitud.cancelar(SOLICITANTE, AHORA);

            assertThatThrownBy(() -> solicitud.enviar(UN_PASO, ASIGNADOR, AHORA))
                .isInstanceOf(TransicionInvalidaException.class)
                .hasMessageContaining("CANCELADA");
        }

        @Test
        void noPermiteEnviarDosVeces() {
            var solicitud = Solicitudes.pendiente(UN_PASO);

            assertThatThrownBy(() -> solicitud.enviar(UN_PASO, ASIGNADOR, AHORA))
                .isInstanceOf(TransicionInvalidaException.class);
        }
    }

    @Nested
    class Aprobacion {

        @Test
        void pasoIntermedioAvanzaAlSiguienteAprobadorYRenuevaLaFechaLimite() {
            var solicitud = Solicitudes.pendiente(DOS_PASOS);
            var despues = AHORA.plus(Duration.ofHours(5));

            solicitud.aprobar(Solicitudes.JEFE, ASIGNADOR, despues);

            assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
            assertThat(solicitud.getPasoActual()).isEqualTo(2);
            assertThat(solicitud.getAprobadorActualId()).isEqualTo(Solicitudes.GERENTE);
            assertThat(solicitud.getFechaLimitePaso()).isEqualTo(despues.plus(LIMITE));
        }

        @Test
        void ultimoPasoDejaLaSolicitudAprobada() {
            var solicitud = Solicitudes.pendiente(DOS_PASOS);
            solicitud.aprobar(Solicitudes.JEFE, ASIGNADOR, AHORA);
            solicitud.pullEventos();

            solicitud.aprobar(Solicitudes.GERENTE, ASIGNADOR, AHORA);

            assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.APROBADA);
            assertThat(solicitud.getAprobadorActualId()).isNull();
            assertThat(solicitud.getFechaLimitePaso()).isNull();
            assertThat(solicitud.pullEventos()).containsExactly(new SolicitudAprobada(solicitud.getId(), AHORA));
        }

        @Test
        void soloPuedeAprobarElAprobadorDelPasoActual() {
            var solicitud = Solicitudes.pendiente(DOS_PASOS);

            assertThatThrownBy(() -> solicitud.aprobar(Solicitudes.GERENTE, ASIGNADOR, AHORA))
                .isInstanceOf(AccionNoPermitidaException.class);
            assertThat(solicitud.getPasoActual()).isEqualTo(1);
        }

        @Test
        void noSePuedeAprobarUnBorrador() {
            var solicitud = Solicitudes.enBorrador();

            assertThatThrownBy(() -> solicitud.aprobar(Solicitudes.JEFE, ASIGNADOR, AHORA))
                .isInstanceOf(TransicionInvalidaException.class);
        }

        @Test
        void unAprobadorQueYaDecidioNoPuedeRepetir() {
            var solicitud = Solicitudes.pendiente(DOS_PASOS);
            solicitud.aprobar(Solicitudes.JEFE, ASIGNADOR, AHORA);

            assertThatThrownBy(() -> solicitud.aprobar(Solicitudes.JEFE, ASIGNADOR, AHORA))
                .isInstanceOf(AccionNoPermitidaException.class);
        }
    }

    @Nested
    class Rechazo {

        @Test
        void dejaLaSolicitudRechazadaYPublicaElEvento() {
            var solicitud = Solicitudes.pendiente(DOS_PASOS);

            solicitud.rechazar(Solicitudes.JEFE, AHORA);

            assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
            assertThat(solicitud.pullEventos()).containsExactly(new SolicitudRechazada(solicitud.getId(), AHORA));
        }

        @Test
        void soloPuedeRechazarElAprobadorDelPasoActual() {
            var solicitud = Solicitudes.pendiente(UN_PASO);

            assertThatThrownBy(() -> solicitud.rechazar(OTRO, AHORA))
                .isInstanceOf(AccionNoPermitidaException.class);
        }

        @Test
        void unaSolicitudRechazadaNoAceptaMasDecisiones() {
            var solicitud = Solicitudes.pendiente(UN_PASO);
            solicitud.rechazar(Solicitudes.JEFE, AHORA);

            assertThatThrownBy(() -> solicitud.aprobar(Solicitudes.JEFE, ASIGNADOR, AHORA))
                .isInstanceOf(TransicionInvalidaException.class)
                .hasMessageContaining("RECHAZADA");
        }
    }

    @Nested
    class Cancelacion {

        @Test
        void desdePendienteConservaElPasoEnElQueSeCancelo() {
            var solicitud = Solicitudes.pendiente(DOS_PASOS);
            solicitud.aprobar(Solicitudes.JEFE, ASIGNADOR, AHORA);

            solicitud.cancelar(SOLICITANTE, AHORA);

            assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.CANCELADA);
            assertThat(solicitud.getPasoActual()).isEqualTo(2);
            assertThat(solicitud.getAprobadorActualId()).isNull();
        }

        @Test
        void soloLaPuedeCancelarElSolicitante() {
            var solicitud = Solicitudes.enBorrador();

            assertThatThrownBy(() -> solicitud.cancelar(OTRO, AHORA))
                .isInstanceOf(AccionNoPermitidaException.class);
        }

        @Test
        void unaSolicitudAprobadaYaNoSePuedeCancelar() {
            var solicitud = Solicitudes.pendiente(UN_PASO);
            solicitud.aprobar(Solicitudes.JEFE, ASIGNADOR, AHORA);

            assertThatThrownBy(() -> solicitud.cancelar(SOLICITANTE, AHORA))
                .isInstanceOf(TransicionInvalidaException.class);
        }
    }

    @Nested
    class Cierre {

        @Test
        void unaSolicitudAprobadaSePuedeCerrar() {
            var solicitud = Solicitudes.pendiente(UN_PASO);
            solicitud.aprobar(Solicitudes.JEFE, ASIGNADOR, AHORA);

            solicitud.cerrar(AHORA);

            assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.CERRADA);
        }

        @Test
        void unaSolicitudPendienteNoSePuedeCerrar() {
            var solicitud = Solicitudes.pendiente(UN_PASO);

            assertThatThrownBy(() -> solicitud.cerrar(AHORA))
                .isInstanceOf(TransicionInvalidaException.class);
        }
    }

    @Nested
    class Escalamiento {

        @Test
        void conPasoSiguienteAvanzaYNoNotificaAlAdmin() {
            var solicitud = Solicitudes.pendiente(DOS_PASOS);

            solicitud.escalar(ASIGNADOR, VENCIDA);

            assertThat(solicitud.getPasoActual()).isEqualTo(2);
            assertThat(solicitud.getAprobadorActualId()).isEqualTo(Solicitudes.GERENTE);
            assertThat(solicitud.getFechaLimitePaso()).isEqualTo(VENCIDA.plus(LIMITE));
            assertThat(solicitud.pullEventos()).containsExactly(
                new SolicitudEscalada(solicitud.getId(), 2, false, VENCIDA));
        }

        @Test
        void enElUltimoPasoNotificaAlAdminYSigueEnPendiente() {
            var solicitud = Solicitudes.pendiente(UN_PASO);

            solicitud.escalar(ASIGNADOR, VENCIDA);

            assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
            assertThat(solicitud.getAprobadorActualId()).isEqualTo(Solicitudes.JEFE);
            assertThat(solicitud.pullEventos()).containsExactly(
                new SolicitudEscalada(solicitud.getId(), 1, true, VENCIDA));
        }

        @Test
        void trasNotificarAlAdminNoVuelveAContarComoVencida() {
            var solicitud = Solicitudes.pendiente(UN_PASO);

            solicitud.escalar(ASIGNADOR, VENCIDA);

            assertThat(solicitud.estaVencida(VENCIDA.plus(Duration.ofDays(30)))).isFalse();
        }

        @Test
        void siElPlazoNoHaVencidoNoHaceNada() {
            var solicitud = Solicitudes.pendiente(DOS_PASOS);

            solicitud.escalar(ASIGNADOR, AHORA.plus(Duration.ofHours(1)));

            assertThat(solicitud.getPasoActual()).isEqualTo(1);
            assertThat(solicitud.pullEventos()).isEmpty();
        }

        @Test
        void unBorradorNoSeEscala() {
            var solicitud = Solicitudes.enBorrador();

            assertThatThrownBy(() -> solicitud.escalar(ASIGNADOR, VENCIDA))
                .isInstanceOf(TransicionInvalidaException.class);
        }
    }

    @Test
    void reconstituirAceptaUnPeriodoPasadoYPermiteContinuarElFlujo() {
        var periodo = new Periodo(HOY.minusDays(30), HOY.minusDays(25));
        var solicitud = Solicitud.reconstituir(SolicitudId.nuevo(), SOLICITANTE, TipoSolicitud.VACACIONES, null, periodo,
            null, EstadoSolicitud.PENDIENTE, UN_PASO.getPasos(), 1, Solicitudes.JEFE, AHORA, 3, AHORA, AHORA);

        solicitud.aprobar(Solicitudes.JEFE, ASIGNADOR, AHORA);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.APROBADA);
        assertThat(solicitud.getVersion()).isEqualTo(3);
    }

    @Nested
    class PasosDelFlujo {

        @Test
        void cambiarElFlujoNoAfectaAUnaSolicitudEnCurso() {
            var flujo = Solicitudes.flujoCon(JEFE, GERENTE);
            var solicitud = Solicitudes.pendiente(flujo);

            flujo.reemplazarPasos(List.of(new PasoAprobacion(1, DIRECTOR, null, 1)));
            solicitud.aprobar(Solicitudes.JEFE, ASIGNADOR, AHORA);

            assertThat(solicitud.getPasoActual()).isEqualTo(2);
            assertThat(solicitud.getAprobadorActualId()).isEqualTo(Solicitudes.GERENTE);
            solicitud.aprobar(Solicitudes.GERENTE, ASIGNADOR, AHORA);
            assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.APROBADA);
        }

        @Test
        void soloCopiaLosPasosCuyoUmbralAlcanza() {
            var flujo = Solicitudes.flujoCon(List.of(
                new PasoAprobacion(1, JEFE, null, 48), new PasoAprobacion(2, GERENTE, new BigDecimal("1000"), 48)));

            var pequena = Solicitudes.compra("999.99");
            pequena.enviar(flujo, ASIGNADOR, AHORA);
            var grande = Solicitudes.compra("1000");
            grande.enviar(flujo, ASIGNADOR, AHORA);

            assertThat(pequena.getPasos()).hasSize(1);
            assertThat(grande.getPasos()).hasSize(2);
        }

        @Test
        void enVacacionesElUmbralSeComparaContraLosDias() {
            var flujo = Solicitudes.flujoCon(List.of(
                new PasoAprobacion(1, JEFE, null, 48), new PasoAprobacion(2, RRHH, new BigDecimal("4"), 48)));

            var tresDias = Solicitud.nuevasVacaciones(SOLICITANTE, new Periodo(HOY, HOY.plusDays(2)), null, HOY, AHORA);
            tresDias.enviar(flujo, ASIGNADOR, AHORA);
            var cuatroDias = Solicitud.nuevasVacaciones(SOLICITANTE, new Periodo(HOY, HOY.plusDays(3)), null, HOY, AHORA);
            cuatroDias.enviar(flujo, ASIGNADOR, AHORA);

            assertThat(tresDias.getPasos()).hasSize(1);
            assertThat(cuatroDias.getPasos()).hasSize(2);
        }

        @Test
        void elInicioDelPasoSeDeduceDeSuFechaLimite() {
            var flujo = Solicitudes.flujoCon(List.of(
                new PasoAprobacion(1, JEFE, null, 24), new PasoAprobacion(2, GERENTE, null, 72)));
            var solicitud = Solicitudes.pendiente(flujo);
            var despues = AHORA.plus(Duration.ofHours(5));

            solicitud.aprobar(Solicitudes.JEFE, ASIGNADOR, despues);

            assertThat(solicitud.inicioPasoActual()).contains(despues);
            assertThat(Solicitudes.enBorrador().inicioPasoActual()).isEmpty();
        }

        @Test
        void cadaPasoUsaSuPropioTiempoLimite() {
            var flujo = Solicitudes.flujoCon(List.of(
                new PasoAprobacion(1, JEFE, null, 24), new PasoAprobacion(2, GERENTE, null, 72)));
            var solicitud = Solicitudes.pendiente(flujo);
            assertThat(solicitud.getFechaLimitePaso()).isEqualTo(AHORA.plus(Duration.ofHours(24)));

            solicitud.aprobar(Solicitudes.JEFE, ASIGNADOR, AHORA);

            assertThat(solicitud.getFechaLimitePaso()).isEqualTo(AHORA.plus(Duration.ofHours(72)));
        }

        @Test
        void unFlujoDesactivadoImpideEnviarYNoCambiaLaSolicitud() {
            var flujo = Flujo.reconstituir(new FlujoId(UUID.randomUUID()), TipoSolicitud.COMPRA, false,
                UN_PASO.getPasos(), 1);
            var solicitud = Solicitudes.enBorrador();

            assertThatThrownBy(() -> solicitud.enviar(flujo, ASIGNADOR, AHORA))
                .isInstanceOf(ReglaNegocioException.class);

            assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
        }
    }

    @Test
    void pullEventosVaciaLaLista() {
        var solicitud = Solicitudes.enBorrador();
        solicitud.enviar(UN_PASO, ASIGNADOR, AHORA);

        assertThat(solicitud.pullEventos()).hasSize(1);
        assertThat(solicitud.pullEventos()).isEmpty();
    }
}
