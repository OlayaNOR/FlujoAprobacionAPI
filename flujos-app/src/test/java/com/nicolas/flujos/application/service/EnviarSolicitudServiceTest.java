package com.nicolas.flujos.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.out.AprobacionRepository;
import com.nicolas.flujos.application.port.out.AuditoriaRepository;
import com.nicolas.flujos.application.port.out.FlujoRepository;
import com.nicolas.flujos.application.port.out.OutboxRepository;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.auditoria.AccionAuditoria;
import com.nicolas.flujos.domain.auditoria.RegistroAuditoria;
import com.nicolas.flujos.domain.flujo.Flujo;
import com.nicolas.flujos.domain.flujo.FlujoId;
import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.flujo.RolAprobador;
import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.EventoDominio.SolicitudAprobada;
import com.nicolas.flujos.domain.solicitud.EventoDominio.SolicitudEnviada;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import com.nicolas.flujos.domain.solicitud.TransicionInvalidaException;
import com.nicolas.flujos.domain.usuario.Rol;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EnviarSolicitudServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T15:30:00Z");
    private static final Currency USD = Currency.getInstance("USD");

    private final SolicitudRepository solicitudes = mock(SolicitudRepository.class);
    private final FlujoRepository flujos = mock(FlujoRepository.class);
    private final AuditoriaRepository auditoria = mock(AuditoriaRepository.class);
    private final OutboxRepository outbox = mock(OutboxRepository.class);
    private final UsuarioId dueno = UsuarioId.nuevo();
    private final UsuarioId jefe = UsuarioId.nuevo();

    private final EnviarSolicitudService servicio = new EnviarSolicitudService(
        new AccesoSolicitudes(solicitudes, mock(AprobacionRepository.class)),
        solicitudes,
        flujos,
        (rol, solicitante) -> jefe,
        auditoria,
        outbox,
        Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void flujos() {
        when(flujos.buscarPorTipo(TipoSolicitud.COMPRA)).thenReturn(Optional.of(flujo(TipoSolicitud.COMPRA, null)));
        when(flujos.buscarPorTipo(TipoSolicitud.GASTO)).thenReturn(Optional.of(flujo(TipoSolicitud.GASTO, "200")));
    }

    @Test
    void registraElEnvioConElSolicitanteComoActor() {
        var solicitud = registrar(Solicitud.nuevaCompra(dueno, Money.de("500", "USD"), null, USD, AHORA));

        var detalle = servicio.ejecutar(solicitud.getId(), actor());

        var registro = registroGuardado();
        assertThat(detalle.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(registro.accion()).isEqualTo(AccionAuditoria.ENVIADA);
        assertThat(registro.actorId()).isEqualTo(dueno);
        assertThat(registro.estadoAnterior()).isEqualTo(EstadoSolicitud.BORRADOR);
        assertThat(registro.estadoNuevo()).isEqualTo(EstadoSolicitud.PENDIENTE);
        verify(outbox).encolar(new SolicitudEnviada(solicitud.getId(), jefe, AHORA));
    }

    @Test
    void unGastoAutomaticoSeRegistraSinActor() {
        var solicitud = registrar(Solicitud.nuevoGasto(dueno, Money.de("50", "USD"), null, USD, AHORA));

        servicio.ejecutar(solicitud.getId(), actor());

        var registro = registroGuardado();
        assertThat(registro.accion()).isEqualTo(AccionAuditoria.APROBADA);
        assertThat(registro.actorId()).isNull();
        assertThat(registro.detalle()).containsEntry("automatica", true);
        verify(outbox).encolar(new SolicitudAprobada(solicitud.getId(), AHORA));
    }

    @Test
    void siLaTransicionFallaNoGuardaNiAudita() {
        var solicitud = registrar(Solicitud.nuevaCompra(dueno, Money.de("500", "USD"), null, USD, AHORA));
        solicitud.cancelar(dueno, AHORA);

        assertThatThrownBy(() -> servicio.ejecutar(solicitud.getId(), actor()))
            .isInstanceOf(TransicionInvalidaException.class);

        verify(solicitudes, never()).guardar(any());
        verify(auditoria, never()).registrar(any());
        verify(outbox, never()).encolar(any());
    }

    @Test
    void sinFlujoConfiguradoNoSePuedeEnviar() {
        when(flujos.buscarPorTipo(TipoSolicitud.COMPRA)).thenReturn(Optional.empty());
        var solicitud = registrar(Solicitud.nuevaCompra(dueno, Money.de("500", "USD"), null, USD, AHORA));

        assertThatThrownBy(() -> servicio.ejecutar(solicitud.getId(), actor()))
            .isInstanceOf(ReglaNegocioException.class);

        verify(solicitudes, never()).guardar(any());
    }

    private static Flujo flujo(TipoSolicitud tipo, String umbral) {
        var paso = new PasoAprobacion(1, RolAprobador.JEFE, umbral == null ? null : new BigDecimal(umbral), 48);
        return Flujo.reconstituir(new FlujoId(UUID.randomUUID()), tipo, true, List.of(paso), 1);
    }

    private Solicitud registrar(Solicitud solicitud) {
        when(solicitudes.buscarPorId(solicitud.getId())).thenReturn(Optional.of(solicitud));
        return solicitud;
    }

    private Actor actor() {
        return new Actor(dueno, Set.of(Rol.EMPLEADO));
    }

    private RegistroAuditoria registroGuardado() {
        var captura = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(auditoria).registrar(captura.capture());
        return captura.getValue();
    }
}
