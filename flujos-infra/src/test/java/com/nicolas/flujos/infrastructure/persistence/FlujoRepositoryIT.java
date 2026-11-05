package com.nicolas.flujos.infrastructure.persistence;

import static com.nicolas.flujos.domain.flujo.RolAprobador.DIRECTOR;
import static com.nicolas.flujos.domain.flujo.RolAprobador.GERENTE;
import static com.nicolas.flujos.domain.flujo.RolAprobador.JEFE;
import static com.nicolas.flujos.domain.flujo.RolAprobador.RRHH;
import static org.assertj.core.api.Assertions.assertThat;

import com.nicolas.flujos.application.port.out.FlujoRepository;
import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.flujo.RolAprobador;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(FlujoRepositoryAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FlujoRepositoryIT {

    private static final String PASSWORD_APP = "clave-app-de-prueba";
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

    @Autowired FlujoRepository repositorio;
    @Autowired PlatformTransactionManager transacciones;

    @Test
    void lasMigracionesDejanLosFlujosIniciales() {
        var compra = repositorio.buscarPorTipo(TipoSolicitud.COMPRA).orElseThrow();
        var vacaciones = repositorio.buscarPorTipo(TipoSolicitud.VACACIONES).orElseThrow();

        assertThat(compra.isActivo()).isTrue();
        assertThat(roles(compra.getPasos())).containsExactly(JEFE, GERENTE, DIRECTOR);
        assertThat(compra.pasosPara(new BigDecimal("10000.00"))).hasSize(2);
        assertThat(compra.pasosPara(new BigDecimal("10000.01"))).hasSize(3);
        assertThat(roles(vacaciones.getPasos())).containsExactly(JEFE, RRHH);
        assertThat(vacaciones.getPasos().get(0).tiempoLimiteHoras()).isEqualTo(48);
    }

    // Trabaja sobre GASTO para no afectar la prueba de los flujos iniciales
    @Test
    void reemplazarLosPasosBorraLosAnterioresAunqueRepitanOrden() {
        var inicial = repositorio.buscarPorTipo(TipoSolicitud.GASTO).orElseThrow().getVersion();

        reemplazar(TipoSolicitud.GASTO, List.of(paso(1, GERENTE, "50"), paso(2, RRHH, null)));
        reemplazar(TipoSolicitud.GASTO, List.of(paso(1, DIRECTOR, null)));

        var leido = repositorio.buscarPorTipo(TipoSolicitud.GASTO).orElseThrow();
        assertThat(roles(leido.getPasos())).containsExactly(DIRECTOR);
        assertThat(leido.getVersion()).isEqualTo(inicial + 2);
    }

    @Test
    void cambiosSimultaneosNoSeMezclan() throws Exception {
        var inicial = repositorio.buscarPorTipo(TipoSolicitud.GASTO).orElseThrow().getVersion();
        var largada = new CountDownLatch(1);

        try (var hilos = Executors.newFixedThreadPool(4)) {
            var resultados = new ArrayList<Future<?>>();
            for (var rol : RolAprobador.values()) {
                resultados.add(hilos.submit(() -> {
                    largada.await();
                    reemplazar(TipoSolicitud.GASTO, List.of(paso(1, rol, null), paso(2, otroRol(rol), null)));
                    return null;
                }));
            }
            largada.countDown();
            for (var resultado : resultados) {
                resultado.get();
            }
        }

        var leido = repositorio.buscarPorTipo(TipoSolicitud.GASTO).orElseThrow();
        assertThat(leido.getPasos()).hasSize(2);
        assertThat(leido.getPasos().get(1).rolAprobador()).isEqualTo(otroRol(leido.getPasos().get(0).rolAprobador()));
        assertThat(leido.getVersion()).isEqualTo(inicial + RolAprobador.values().length);
    }

    private void reemplazar(TipoSolicitud tipo, List<PasoAprobacion> pasos) {
        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            var flujo = repositorio.buscarParaActualizar(tipo).orElseThrow();
            flujo.reemplazarPasos(pasos);
            repositorio.guardar(flujo);
        });
    }

    private static RolAprobador otroRol(RolAprobador rol) {
        var roles = RolAprobador.values();
        return roles[(rol.ordinal() + 1) % roles.length];
    }

    private static PasoAprobacion paso(int orden, RolAprobador rol, String umbral) {
        return new PasoAprobacion(orden, rol, umbral == null ? null : new BigDecimal(umbral), 24);
    }

    private static List<RolAprobador> roles(List<PasoAprobacion> pasos) {
        return pasos.stream().map(PasoAprobacion::rolAprobador).toList();
    }
}
