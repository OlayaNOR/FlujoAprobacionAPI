package com.nicolas.flujos.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nicolas.flujos.application.port.in.CampoOrden;
import com.nicolas.flujos.application.port.in.FiltroSolicitud;
import com.nicolas.flujos.application.port.in.PaginaSolicitada;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.shared.Periodo;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SolicitudRepositoryAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SolicitudRepositoryIT {

    private static final String PASSWORD_APP = "clave-app-de-prueba";
    private static final Currency USD = Currency.getInstance("USD");
    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MICROS);
    private static final LocalDate HOY = LocalDate.parse("2026-11-05");

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        PG.start();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", () -> "flujos_app");
        r.add("spring.datasource.password", () -> PASSWORD_APP);
        r.add("spring.flyway.user", PG::getUsername);
        r.add("spring.flyway.password", PG::getPassword);
        r.add("spring.flyway.placeholders.appPassword", () -> PASSWORD_APP);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired SolicitudRepository repositorio;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transacciones;

    @Test
    void guardaYRecuperaUnaCompraConTodosSusDatos() {
        var solicitante = crearUsuario();
        var solicitud = compra(solicitante, "1250.50");

        guardar(solicitud);
        var leida = repositorio.buscarPorId(solicitud.getId()).orElseThrow();

        assertThat(leida.getSolicitanteId()).isEqualTo(solicitante);
        assertThat(leida.getTipo()).isEqualTo(TipoSolicitud.COMPRA);
        assertThat(leida.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
        assertThat(leida.getMonto()).isEqualTo(Money.de("1250.50", "USD"));
        assertThat(leida.getPeriodo()).isNull();
        assertThat(leida.getMotivo()).isEqualTo("Licencias");
        assertThat(leida.getCreadaEn()).isEqualTo(AHORA);
        assertThat(leida.getVersion()).isZero();
    }

    @Test
    void guardaYRecuperaUnasVacaciones() {
        var periodo = new Periodo(HOY, HOY.plusDays(4));
        var solicitud = Solicitud.nuevasVacaciones(crearUsuario(), periodo, null, HOY, AHORA);

        guardar(solicitud);
        var leida = repositorio.buscarPorId(solicitud.getId()).orElseThrow();

        assertThat(leida.getPeriodo()).isEqualTo(periodo);
        assertThat(leida.getMonto()).isNull();
    }

    @Test
    void cadaCambioGuardadoAumentaLaVersion() {
        var solicitud = compra(crearUsuario(), "100");
        guardar(solicitud);

        editarYGuardar(solicitud, "200");

        var leida = repositorio.buscarPorId(solicitud.getId()).orElseThrow();
        assertThat(leida.getMonto()).isEqualTo(Money.de("200", "USD"));
        assertThat(leida.getVersion()).isEqualTo(1);
    }

    @Test
    void dosTransaccionesSobreLaMismaSolicitudLaSegundaFalla() {
        var solicitud = compra(crearUsuario(), "100");
        guardar(solicitud);
        var primera = new TransactionTemplate(transacciones);

        assertThatThrownBy(() -> primera.executeWithoutResult(estado -> {
            var cargada = repositorio.buscarPorId(solicitud.getId()).orElseThrow();
            editarEnTransaccionNueva(solicitud, "300");
            cargada.editarMonto(Money.de("400", "USD"), null, USD, AHORA);
            repositorio.guardar(cargada);
        })).isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(repositorio.buscarPorId(solicitud.getId()).orElseThrow().getMonto())
            .isEqualTo(Money.de("300", "USD"));
    }

    @Test
    void guardarFueraDeUnaTransaccionNoEstaPermitido() {
        var solicitud = compra(crearUsuario(), "100");

        assertThatThrownBy(() -> repositorio.guardar(solicitud))
            .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void listarSoloMuestraLasDelUsuarioOLasQueTienePorAprobar() {
        var ana = crearUsuario();
        var luis = crearUsuario();
        var deAna = compra(ana, "100");
        var deLuis = compra(luis, "200");
        var deLuisParaAna = compra(luis, "300");
        guardar(deAna);
        guardar(deLuis);
        guardar(deLuisParaAna);
        jdbc.update("UPDATE solicitud SET aprobador_actual_id = ? WHERE id = ?", ana.valor(), deLuisParaAna.getId().valor());

        var pagina = repositorio.listar(sinFiltro(), ana, porMontoAscendente(0, 10));

        assertThat(pagina.content()).extracting(Solicitud::getId)
            .containsExactly(deAna.getId(), deLuisParaAna.getId());
        assertThat(pagina.totalElements()).isEqualTo(2);
    }

    @Test
    void filtraPorEstadoYTipo() {
        var ana = crearUsuario();
        var compra = compra(ana, "100");
        var gasto = Solicitud.nuevoGasto(ana, Money.de("50", "USD"), null, USD, AHORA);
        guardar(compra);
        guardar(gasto);

        var porTipo = new FiltroSolicitud(Set.of(), TipoSolicitud.GASTO, null, null);
        var porEstado = new FiltroSolicitud(Set.of(EstadoSolicitud.APROBADA), null, null, null);

        assertThat(repositorio.listar(porTipo, ana, porMontoAscendente(0, 10)).content())
            .extracting(Solicitud::getId).containsExactly(gasto.getId());
        assertThat(repositorio.listar(porEstado, ana, porMontoAscendente(0, 10)).content()).isEmpty();
    }

    @Test
    void filtraPorRangoDeFechasDeCreacion() {
        var ana = crearUsuario();
        var solicitud = compra(ana, "100");
        guardar(solicitud);
        var unaHora = Duration.ofHours(1);

        var dentro = new FiltroSolicitud(Set.of(), null, AHORA.minus(unaHora), AHORA.plus(unaHora));
        var despues = new FiltroSolicitud(Set.of(), null, AHORA.plus(unaHora), null);

        assertThat(repositorio.listar(dentro, ana, porMontoAscendente(0, 10)).content()).hasSize(1);
        assertThat(repositorio.listar(despues, ana, porMontoAscendente(0, 10)).content()).isEmpty();
    }

    @Test
    void paginaYOrdenaLosResultados() {
        var ana = crearUsuario();
        for (var monto : List.of("300", "100", "200")) {
            guardar(compra(ana, monto));
        }

        var primera = repositorio.listar(sinFiltro(), ana, porMontoAscendente(0, 2));
        var segunda = repositorio.listar(sinFiltro(), ana, porMontoAscendente(1, 2));

        assertThat(primera.content()).extracting(Solicitud::getMonto)
            .containsExactly(Money.de("100", "USD"), Money.de("200", "USD"));
        assertThat(segunda.content()).extracting(Solicitud::getMonto).containsExactly(Money.de("300", "USD"));
        assertThat(primera.totalElements()).isEqualTo(3);
        assertThat(primera.totalPages()).isEqualTo(2);
    }

    private void guardar(Solicitud solicitud) {
        new TransactionTemplate(transacciones).executeWithoutResult(estado -> repositorio.guardar(solicitud));
    }

    private void editarYGuardar(Solicitud solicitud, String monto) {
        new TransactionTemplate(transacciones).executeWithoutResult(estado -> editar(solicitud, monto));
    }

    private void editarEnTransaccionNueva(Solicitud solicitud, String monto) {
        var nueva = new TransactionTemplate(transacciones);
        nueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        nueva.executeWithoutResult(estado -> editar(solicitud, monto));
    }

    private void editar(Solicitud solicitud, String monto) {
        var cargada = repositorio.buscarPorId(solicitud.getId()).orElseThrow();
        cargada.editarMonto(Money.de(monto, "USD"), null, USD, AHORA);
        repositorio.guardar(cargada);
    }

    private static FiltroSolicitud sinFiltro() {
        return new FiltroSolicitud(Set.of(), null, null, null);
    }

    private static PaginaSolicitada porMontoAscendente(int numero, int tamano) {
        return new PaginaSolicitada(numero, tamano, CampoOrden.MONTO, true);
    }

    private static Solicitud compra(UsuarioId solicitante, String monto) {
        return Solicitud.nuevaCompra(solicitante, Money.de(monto, "USD"), "Licencias", USD, AHORA);
    }

    private UsuarioId crearUsuario() {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario (id, email, nombre, password_hash) VALUES (?, ?, 'Prueba', 'hash')",
            id, id + "@flujos.test");
        return new UsuarioId(id);
    }
}
