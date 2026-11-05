package com.nicolas.flujos.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.in.DecidirSolicitudCommand;
import com.nicolas.flujos.application.port.in.TipoDecision;
import com.nicolas.flujos.application.port.out.AprobacionRepository;
import com.nicolas.flujos.application.port.out.AuditoriaRepository;
import com.nicolas.flujos.application.port.out.OutboxRepository;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.auditoria.AccionAuditoria;
import com.nicolas.flujos.domain.auditoria.RegistroAuditoria;
import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.flujo.RolAprobador;
import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import com.nicolas.flujos.domain.solicitud.AccionNoPermitidaException;
import com.nicolas.flujos.domain.solicitud.Decision;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.EventoDominio.SolicitudRechazada;
import com.nicolas.flujos.domain.solicitud.ResultadoDecision;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import com.nicolas.flujos.domain.usuario.Rol;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DecidirSolicitudServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T15:30:00Z");

    private final SolicitudRepository solicitudes = mock(SolicitudRepository.class);
    private final AprobacionRepository aprobaciones = mock(AprobacionRepository.class);
    private final AuditoriaRepository auditoria = mock(AuditoriaRepository.class);
    private final OutboxRepository outbox = mock(OutboxRepository.class);
    private final UsuarioId dueno = UsuarioId.nuevo();
    private final UsuarioId jefe = UsuarioId.nuevo();
    private final UsuarioId gerente = UsuarioId.nuevo();

    private final DecidirSolicitudService servicio = new DecidirSolicitudService(
        new AccesoSolicitudes(solicitudes, aprobaciones), solicitudes, aprobaciones, auditoria, outbox,
        (rol, solicitante) -> gerente, Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void aprobarUnPasoIntermedioGuardaLaDecisionDeEsePaso() {
        var solicitud = pendiente(RolAprobador.JEFE, RolAprobador.GERENTE);

        var detalle = servicio.ejecutar(comando(solicitud, jefe, TipoDecision.APROBAR, "Visto bueno"));

        assertThat(detalle.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(detalle.pasoActual()).isEqualTo(2);
        assertThat(detalle.aprobadorActualId()).isEqualTo(gerente);
        var decision = decisionGuardada();
        assertThat(decision.paso()).isEqualTo(1);
        assertThat(decision.resultado()).isEqualTo(ResultadoDecision.APROBADA);
        assertThat(decision.aprobadorId()).isEqualTo(jefe);
        var registro = registroGuardado();
        assertThat(registro.accion()).isEqualTo(AccionAuditoria.APROBADA);
        assertThat(registro.estadoNuevo()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(registro.detalle()).containsEntry("paso", 1).containsEntry("comentario", "Visto bueno");
    }

    @Test
    void rechazarDejaLaSolicitudRechazadaConSuMotivo() {
        var solicitud = pendiente(RolAprobador.JEFE);

        var detalle = servicio.ejecutar(comando(solicitud, jefe, TipoDecision.RECHAZAR, "Sin presupuesto"));

        assertThat(detalle.estado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(decisionGuardada().resultado()).isEqualTo(ResultadoDecision.RECHAZADA);
        assertThat(registroGuardado().accion()).isEqualTo(AccionAuditoria.RECHAZADA);
        verify(outbox).encolar(new SolicitudRechazada(solicitud.getId(), AHORA));
    }

    @Test
    void rechazarSinMotivoNoGuardaNada() {
        var solicitud = pendiente(RolAprobador.JEFE);

        assertThatThrownBy(() -> servicio.ejecutar(comando(solicitud, jefe, TipoDecision.RECHAZAR, " ")))
            .isInstanceOf(ReglaNegocioException.class);

        verify(solicitudes, never()).guardar(any());
        verify(aprobaciones, never()).registrar(any());
        verify(outbox, never()).encolar(any());
    }

    @Test
    void quienVeLaSolicitudPeroNoEsElAprobadorNoPuedeDecidir() {
        var solicitud = pendiente(RolAprobador.JEFE);

        assertThatThrownBy(() -> servicio.ejecutar(comando(solicitud, dueno, TipoDecision.APROBAR, null)))
            .isInstanceOf(AccionNoPermitidaException.class);

        verify(aprobaciones, never()).registrar(any());
    }

    private Solicitud pendiente(RolAprobador... roles) {
        var pasos = new ArrayList<PasoAprobacion>();
        for (var rol : roles) {
            pasos.add(new PasoAprobacion(pasos.size() + 1, rol, null, 48));
        }
        var solicitud = Solicitud.reconstituir(SolicitudId.nuevo(), dueno, TipoSolicitud.COMPRA, Money.de("500", "USD"),
            null, null, EstadoSolicitud.PENDIENTE, pasos, 1, jefe, AHORA, 1, AHORA, AHORA);
        when(solicitudes.buscarPorId(solicitud.getId())).thenReturn(Optional.of(solicitud));
        return solicitud;
    }

    private static DecidirSolicitudCommand comando(Solicitud solicitud, UsuarioId actor, TipoDecision tipo, String comentario) {
        return new DecidirSolicitudCommand(solicitud.getId(), new Actor(actor, Set.of(Rol.APROBADOR)), tipo, comentario);
    }

    private Decision decisionGuardada() {
        var captura = ArgumentCaptor.forClass(Decision.class);
        verify(aprobaciones).registrar(captura.capture());
        return captura.getValue();
    }

    private RegistroAuditoria registroGuardado() {
        var captura = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(auditoria).registrar(captura.capture());
        return captura.getValue();
    }
}
