package com.nicolas.flujos.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nicolas.flujos.application.port.out.AuditoriaRepository;
import com.nicolas.flujos.application.port.out.OutboxRepository;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.auditoria.AccionAuditoria;
import com.nicolas.flujos.domain.auditoria.RegistroAuditoria;
import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.flujo.RolAprobador;
import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.EventoDominio.SolicitudEscalada;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

class EscalarVencidasServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T15:30:00Z");
    private static final Instant VENCIO = AHORA.minus(Duration.ofMinutes(5));

    private final SolicitudRepository solicitudes = mock(SolicitudRepository.class);
    private final AuditoriaRepository auditoria = mock(AuditoriaRepository.class);
    private final OutboxRepository outbox = mock(OutboxRepository.class);
    private final UsuarioId jefe = UsuarioId.nuevo();
    private final UsuarioId gerente = UsuarioId.nuevo();

    private final EscalarVencidasService servicio = new EscalarVencidasService(solicitudes, auditoria, outbox,
        (rol, solicitante) -> {
            if (rol == RolAprobador.DIRECTOR) {
                throw new ReglaNegocioException("No hay un aprobador activo con cargo DIRECTOR");
            }
            return gerente;
        },
        mock(PlatformTransactionManager.class), Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void conPasoSiguienteLaSolicitudAvanzaYQuedaAuditadaSinActor() {
        var solicitud = vencida(RolAprobador.JEFE, RolAprobador.GERENTE);

        var resultado = servicio.ejecutar();

        assertThat(resultado.escaladas()).isEqualTo(1);
        assertThat(solicitud.getPasoActual()).isEqualTo(2);
        var registro = registrosGuardados().get(0);
        assertThat(registro.accion()).isEqualTo(AccionAuditoria.ESCALADA);
        assertThat(registro.actorId()).isNull();
        assertThat(registro.detalle()).containsEntry("pasoAnterior", 1).containsEntry("paso", 2);
        verify(outbox).encolar(new SolicitudEscalada(solicitud.getId(), 2, false, AHORA));
    }

    @Test
    void enElUltimoPasoSeAvisaAlAdmin() {
        var solicitud = vencida(RolAprobador.JEFE);

        servicio.ejecutar();

        assertThat(solicitud.getPasoActual()).isEqualTo(1);
        assertThat(solicitud.getFechaLimitePaso()).isNull();
        assertThat(registrosGuardados().get(0).accion()).isEqualTo(AccionAuditoria.ESCALADA_ADMIN);
        verify(outbox).encolar(new SolicitudEscalada(solicitud.getId(), 1, true, AHORA));
    }

    @Test
    void unaQueFallaNoImpideEscalarLasDemas() {
        var sinDirector = solicitud(RolAprobador.JEFE, RolAprobador.DIRECTOR);
        var normal = solicitud(RolAprobador.JEFE, RolAprobador.GERENTE);
        cuandoVencidas(sinDirector, normal);

        var resultado = servicio.ejecutar();

        assertThat(resultado.escaladas()).isEqualTo(1);
        assertThat(resultado.fallidas()).singleElement().satisfies(fallo -> {
            assertThat(fallo.solicitudId()).isEqualTo(sinDirector.getId());
            assertThat(fallo.motivo()).contains("DIRECTOR");
        });
        assertThat(normal.getPasoActual()).isEqualTo(2);
    }

    @Test
    void siOtraInstanciaLaTomoNoLaCuenta() {
        var id = SolicitudId.nuevo();
        when(solicitudes.vencidas(AHORA, 100)).thenReturn(List.of(id));
        when(solicitudes.tomarVencida(id, AHORA)).thenReturn(Optional.empty());

        assertThat(servicio.ejecutar().escaladas()).isZero();

        verify(solicitudes, never()).guardar(any());
        verify(auditoria, never()).registrar(any());
    }

    private Solicitud vencida(RolAprobador... roles) {
        var solicitud = solicitud(roles);
        cuandoVencidas(solicitud);
        return solicitud;
    }

    private void cuandoVencidas(Solicitud... vencidas) {
        var ids = new ArrayList<SolicitudId>();
        for (var solicitud : vencidas) {
            ids.add(solicitud.getId());
            when(solicitudes.tomarVencida(solicitud.getId(), AHORA)).thenReturn(Optional.of(solicitud));
        }
        when(solicitudes.vencidas(AHORA, 100)).thenReturn(ids);
    }

    private Solicitud solicitud(RolAprobador... roles) {
        var pasos = new ArrayList<PasoAprobacion>();
        for (var rol : roles) {
            pasos.add(new PasoAprobacion(pasos.size() + 1, rol, null, 48));
        }
        return Solicitud.reconstituir(SolicitudId.nuevo(), UsuarioId.nuevo(), TipoSolicitud.COMPRA,
            Money.de("500", "USD"), null, null, EstadoSolicitud.PENDIENTE, pasos, 1, jefe, VENCIO, 1, AHORA, AHORA);
    }

    private List<RegistroAuditoria> registrosGuardados() {
        var captura = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(auditoria, atLeastOnce()).registrar(captura.capture());
        return captura.getAllValues();
    }
}
