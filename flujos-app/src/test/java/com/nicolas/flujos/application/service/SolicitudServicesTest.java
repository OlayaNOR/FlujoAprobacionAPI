package com.nicolas.flujos.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.in.CrearSolicitudCommand;
import com.nicolas.flujos.application.port.in.EditarSolicitudCommand;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.shared.Periodo;
import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import com.nicolas.flujos.domain.solicitud.AccionNoPermitidaException;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import com.nicolas.flujos.domain.solicitud.SolicitudNoEncontradaException;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import com.nicolas.flujos.domain.usuario.Rol;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SolicitudServicesTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T15:30:00Z");
    private static final LocalDate HOY = LocalDate.parse("2026-11-05");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);
    private static final Currency USD = Currency.getInstance("USD");
    private static final Money CIEN = Money.de("100", "USD");

    private final SolicitudRepository repositorio = mock(SolicitudRepository.class);
    private final AccesoSolicitudes acceso = new AccesoSolicitudes(repositorio);
    private final CrearSolicitudService crear = new CrearSolicitudService(repositorio, RELOJ, USD);
    private final ConsultarSolicitudService consultar = new ConsultarSolicitudService(acceso);
    private final EditarSolicitudService editar = new EditarSolicitudService(acceso, repositorio, RELOJ, USD);

    private final UsuarioId dueno = UsuarioId.nuevo();
    private final UsuarioId aprobador = UsuarioId.nuevo();

    @Test
    void crearGuardaLaSolicitudEnBorrador() {
        var detalle = crear.ejecutar(new CrearSolicitudCommand(dueno, TipoSolicitud.COMPRA, CIEN, null, "Licencias"));

        verify(repositorio).guardar(any());
        assertThat(detalle.estado()).isEqualTo(EstadoSolicitud.BORRADOR);
        assertThat(detalle.creadaEn()).isEqualTo(AHORA);
    }

    @Test
    void crearVacacionesUsaLaFechaDelReloj() {
        var pasado = new Periodo(HOY.minusDays(1), HOY);

        assertThatThrownBy(() -> crear.ejecutar(new CrearSolicitudCommand(dueno, TipoSolicitud.VACACIONES, null, pasado, null)))
            .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void unaCompraConPeriodoSeRechazaEnVezDeIgnorarlo() {
        var periodo = new Periodo(HOY, HOY);

        assertThatThrownBy(() -> crear.ejecutar(new CrearSolicitudCommand(dueno, TipoSolicitud.COMPRA, CIEN, periodo, null)))
            .isInstanceOf(ReglaNegocioException.class);
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void unasVacacionesConMontoSeRechazanEnVezDeIgnorarlo() {
        var periodo = new Periodo(HOY, HOY);

        assertThatThrownBy(() -> crear.ejecutar(new CrearSolicitudCommand(dueno, TipoSolicitud.VACACIONES, CIEN, periodo, null)))
            .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void elDuenoElAprobadorAsignadoYElAdminPuedenConsultar() {
        var solicitud = pendiente();

        assertThat(consultar.ejecutar(solicitud.getId(), actor(dueno, Rol.EMPLEADO)).id()).isEqualTo(solicitud.getId());
        assertThat(consultar.ejecutar(solicitud.getId(), actor(aprobador, Rol.APROBADOR)).id()).isEqualTo(solicitud.getId());
        assertThat(consultar.ejecutar(solicitud.getId(), actor(UsuarioId.nuevo(), Rol.ADMIN)).id()).isEqualTo(solicitud.getId());
    }

    @Test
    void unTerceroRecibeNoEncontradaAunqueLaSolicitudExista() {
        var solicitud = pendiente();

        assertThatThrownBy(() -> consultar.ejecutar(solicitud.getId(), actor(UsuarioId.nuevo(), Rol.EMPLEADO)))
            .isInstanceOf(SolicitudNoEncontradaException.class);
    }

    @Test
    void unaSolicitudInexistenteDaNoEncontrada() {
        var id = SolicitudId.nuevo();
        when(repositorio.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultar.ejecutar(id, actor(UsuarioId.nuevo(), Rol.ADMIN)))
            .isInstanceOf(SolicitudNoEncontradaException.class);
    }

    @Test
    void elDuenoPuedeEditarSuBorrador() {
        var solicitud = borrador();

        var detalle = editar.ejecutar(new EditarSolicitudCommand(solicitud.getId(), actor(dueno, Rol.EMPLEADO),
            Money.de("250", "USD"), null, "Otro"));

        verify(repositorio).guardar(solicitud);
        assertThat(detalle.monto()).isEqualTo(Money.de("250", "USD"));
        assertThat(detalle.motivo()).isEqualTo("Otro");
    }

    @Test
    void elAdminPuedeVerPeroNoEditarUnaSolicitudAjena() {
        var solicitud = borrador();

        assertThatThrownBy(() -> editar.ejecutar(new EditarSolicitudCommand(solicitud.getId(),
            actor(UsuarioId.nuevo(), Rol.ADMIN), CIEN, null, null)))
            .isInstanceOf(AccionNoPermitidaException.class);
    }

    @Test
    void unTerceroQueIntentaEditarRecibeNoEncontrada() {
        var solicitud = borrador();

        assertThatThrownBy(() -> editar.ejecutar(new EditarSolicitudCommand(solicitud.getId(),
            actor(UsuarioId.nuevo(), Rol.EMPLEADO), CIEN, null, null)))
            .isInstanceOf(SolicitudNoEncontradaException.class);
    }

    private Solicitud borrador() {
        var solicitud = Solicitud.nuevaCompra(dueno, CIEN, null, USD, AHORA);
        when(repositorio.buscarPorId(solicitud.getId())).thenReturn(Optional.of(solicitud));
        return solicitud;
    }

    private Solicitud pendiente() {
        var solicitud = Solicitud.reconstituir(SolicitudId.nuevo(), dueno, TipoSolicitud.COMPRA, CIEN, null, null,
            EstadoSolicitud.PENDIENTE, 1, aprobador, AHORA, 1, AHORA, AHORA);
        when(repositorio.buscarPorId(solicitud.getId())).thenReturn(Optional.of(solicitud));
        return solicitud;
    }

    private static Actor actor(UsuarioId id, Rol rol) {
        return new Actor(id, Set.of(rol));
    }
}
