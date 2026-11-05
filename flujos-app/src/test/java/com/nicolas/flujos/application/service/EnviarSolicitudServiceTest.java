package com.nicolas.flujos.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.out.AuditoriaRepository;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.auditoria.AccionAuditoria;
import com.nicolas.flujos.domain.auditoria.RegistroAuditoria;
import com.nicolas.flujos.domain.regla.ReglaCompra;
import com.nicolas.flujos.domain.regla.ReglaGasto;
import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.TransicionInvalidaException;
import com.nicolas.flujos.domain.usuario.Rol;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EnviarSolicitudServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T15:30:00Z");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Duration LIMITE = Duration.ofHours(48);

    private final SolicitudRepository solicitudes = mock(SolicitudRepository.class);
    private final AuditoriaRepository auditoria = mock(AuditoriaRepository.class);
    private final UsuarioId dueno = UsuarioId.nuevo();
    private final UsuarioId jefe = UsuarioId.nuevo();

    private final EnviarSolicitudService servicio = new EnviarSolicitudService(
        new AccesoSolicitudes(solicitudes),
        solicitudes,
        new ReglasAprobacionRegistry(List.of(new ReglaCompra(LIMITE), new ReglaGasto(LIMITE))),
        (rol, solicitante) -> jefe,
        auditoria,
        Clock.fixed(AHORA, ZoneOffset.UTC));

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
    }

    @Test
    void unGastoAutomaticoSeRegistraSinActor() {
        var solicitud = registrar(Solicitud.nuevoGasto(dueno, Money.de("50", "USD"), null, USD, AHORA));

        servicio.ejecutar(solicitud.getId(), actor());

        var registro = registroGuardado();
        assertThat(registro.accion()).isEqualTo(AccionAuditoria.APROBADA);
        assertThat(registro.actorId()).isNull();
        assertThat(registro.detalle()).containsEntry("automatica", true);
    }

    @Test
    void siLaTransicionFallaNoGuardaNiAudita() {
        var solicitud = registrar(Solicitud.nuevaCompra(dueno, Money.de("500", "USD"), null, USD, AHORA));
        solicitud.cancelar(dueno, AHORA);

        assertThatThrownBy(() -> servicio.ejecutar(solicitud.getId(), actor()))
            .isInstanceOf(TransicionInvalidaException.class);

        verify(solicitudes, never()).guardar(any());
        verify(auditoria, never()).registrar(any());
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
