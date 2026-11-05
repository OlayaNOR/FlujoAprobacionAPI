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
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.auditoria.AccionAuditoria;
import com.nicolas.flujos.domain.auditoria.RegistroAuditoria;
import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.flujo.RolAprobador;
import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.solicitud.AccionNoPermitidaException;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import com.nicolas.flujos.domain.solicitud.SolicitudNoEncontradaException;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import com.nicolas.flujos.domain.solicitud.TransicionInvalidaException;
import com.nicolas.flujos.domain.usuario.Rol;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CancelarYCerrarServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T15:30:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);

    private final SolicitudRepository repositorio = mock(SolicitudRepository.class);
    private final AuditoriaRepository auditoria = mock(AuditoriaRepository.class);
    private final AccesoSolicitudes acceso = new AccesoSolicitudes(repositorio, mock(AprobacionRepository.class));
    private final CancelarSolicitudService cancelar = new CancelarSolicitudService(acceso, repositorio, auditoria, RELOJ);
    private final CerrarSolicitudService cerrar = new CerrarSolicitudService(acceso, repositorio, auditoria, RELOJ);
    private final ConsultarAuditoriaService consultarAuditoria = new ConsultarAuditoriaService(acceso, auditoria);

    private final UsuarioId dueno = UsuarioId.nuevo();
    private final UsuarioId aprobador = UsuarioId.nuevo();
    private final UsuarioId admin = UsuarioId.nuevo();

    @Test
    void cancelarDejaEnLaAuditoriaElPasoEnQueIba() {
        var solicitud = guardada(EstadoSolicitud.PENDIENTE, aprobador);

        var detalle = cancelar.ejecutar(solicitud.getId(), actor(dueno, Rol.EMPLEADO));

        assertThat(detalle.estado()).isEqualTo(EstadoSolicitud.CANCELADA);
        verify(repositorio).guardar(solicitud);
        var registro = registroGuardado();
        assertThat(registro.accion()).isEqualTo(AccionAuditoria.CANCELADA);
        assertThat(registro.actorId()).isEqualTo(dueno);
        assertThat(registro.estadoAnterior()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(registro.detalle()).isEqualTo(Map.of("paso", 1));
    }

    @Test
    void elAprobadorVeLaSolicitudPeroNoPuedeCancelarla() {
        var solicitud = guardada(EstadoSolicitud.PENDIENTE, aprobador);

        assertThatThrownBy(() -> cancelar.ejecutar(solicitud.getId(), actor(aprobador, Rol.APROBADOR)))
            .isInstanceOf(AccionNoPermitidaException.class);
        verify(repositorio, never()).guardar(any());
        verify(auditoria, never()).registrar(any());
    }

    @Test
    void cerrarUnaAprobadaQuedaAuditadoConElAdmin() {
        var solicitud = guardada(EstadoSolicitud.APROBADA, null);

        var detalle = cerrar.ejecutar(solicitud.getId(), actor(admin, Rol.ADMIN));

        assertThat(detalle.estado()).isEqualTo(EstadoSolicitud.CERRADA);
        var registro = registroGuardado();
        assertThat(registro.accion()).isEqualTo(AccionAuditoria.CERRADA);
        assertThat(registro.actorId()).isEqualTo(admin);
        assertThat(registro.estadoAnterior()).isEqualTo(EstadoSolicitud.APROBADA);
    }

    @Test
    void unaPendienteNoSePuedeCerrar() {
        var solicitud = guardada(EstadoSolicitud.PENDIENTE, aprobador);

        assertThatThrownBy(() -> cerrar.ejecutar(solicitud.getId(), actor(admin, Rol.ADMIN)))
            .isInstanceOf(TransicionInvalidaException.class);
        verify(auditoria, never()).registrar(any());
    }

    @Test
    void laAuditoriaSeVeConLosMismosPermisosQueLaSolicitud() {
        var solicitud = guardada(EstadoSolicitud.PENDIENTE, aprobador);
        var historial = List.of(new RegistroAuditoria(solicitud.getId(), AccionAuditoria.ENVIADA, dueno,
            EstadoSolicitud.BORRADOR, EstadoSolicitud.PENDIENTE, Map.of(), AHORA));
        when(auditoria.listarPorSolicitud(solicitud.getId())).thenReturn(historial);

        assertThat(consultarAuditoria.ejecutar(solicitud.getId(), actor(dueno, Rol.EMPLEADO))).isEqualTo(historial);
        assertThatThrownBy(() -> consultarAuditoria.ejecutar(solicitud.getId(), actor(UsuarioId.nuevo(), Rol.EMPLEADO)))
            .isInstanceOf(SolicitudNoEncontradaException.class);
    }

    private Solicitud guardada(EstadoSolicitud estado, UsuarioId aprobadorActual) {
        var solicitud = Solicitud.reconstituir(SolicitudId.nuevo(), dueno, TipoSolicitud.COMPRA, Money.de("100", "USD"),
            null, null, estado, List.of(new PasoAprobacion(1, RolAprobador.JEFE, null, 48)), 1, aprobadorActual,
            aprobadorActual == null ? null : AHORA.plusSeconds(3600), 1, AHORA, AHORA);
        when(repositorio.buscarPorId(solicitud.getId())).thenReturn(Optional.of(solicitud));
        return solicitud;
    }

    private RegistroAuditoria registroGuardado() {
        var captor = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(auditoria).registrar(captor.capture());
        return captor.getValue();
    }

    private static Actor actor(UsuarioId id, Rol rol) {
        return new Actor(id, Set.of(rol));
    }
}
