package com.nicolas.flujos.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.flujo.RolAprobador;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Las solicitudes en curso antes de V7 no tenían copia de sus pasos: sin ella no podrían avanzar.
@Testcontainers
class MigracionPasosIT {

    private static final UUID USUARIO = UUID.randomUUID();

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void lasSolicitudesEnCursoRecibenLosPasosDeLasReglasAnteriores() throws Exception {
        migrarHasta("6");
        var compraPequena = insertar("COMPRA", "PENDIENTE", "999.99", null, null);
        var compraMedia = insertar("COMPRA", "PENDIENTE", "10000", null, null);
        var compraGrande = insertar("COMPRA", "PENDIENTE", "10000.01", null, null);
        var gasto = insertar("GASTO", "PENDIENTE", "200", null, null);
        var vacacionesCortas = insertar("VACACIONES", "PENDIENTE", null, "2027-02-01", "2027-02-03");
        var vacacionesLargas = insertar("VACACIONES", "PENDIENTE", null, "2027-02-01", "2027-02-04");
        var borrador = insertar("COMPRA", "BORRADOR", "20000", null, null);

        migrarHasta("7");

        assertThat(roles(compraPequena)).containsExactly(RolAprobador.JEFE);
        assertThat(roles(compraMedia)).containsExactly(RolAprobador.JEFE, RolAprobador.GERENTE);
        assertThat(roles(compraGrande)).containsExactly(RolAprobador.JEFE, RolAprobador.GERENTE, RolAprobador.DIRECTOR);
        assertThat(roles(gasto)).containsExactly(RolAprobador.JEFE);
        assertThat(roles(vacacionesCortas)).containsExactly(RolAprobador.JEFE);
        assertThat(roles(vacacionesLargas)).containsExactly(RolAprobador.JEFE, RolAprobador.RRHH);
        assertThat(roles(borrador)).isEmpty();
        assertThat(pasos(compraGrande)).extracting(PasoAprobacion::orden).containsExactly(1, 2, 3);
        assertThat(pasos(compraGrande)).extracting(PasoAprobacion::tiempoLimiteHoras).containsOnly(48);
    }

    private static void migrarHasta(String version) throws SQLException {
        Flyway.configure()
            .dataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())
            .placeholders(Map.of("appPassword", "clave-de-prueba"))
            .target(version)
            .load()
            .migrate();
        try (var c = conexion(); var st = c.createStatement()) {
            st.executeUpdate("""
                INSERT INTO usuario (id, email, nombre, password_hash)
                VALUES ('%s', 'x@flujos.test', 'X', 'hash') ON CONFLICT DO NOTHING
                """.formatted(USUARIO));
        }
    }

    private static UUID insertar(String tipo, String estado, String monto, String inicio, String fin) throws SQLException {
        var id = UUID.randomUUID();
        try (var c = conexion(); var st = c.prepareStatement("""
            INSERT INTO solicitud (id, solicitante_id, tipo, estado, monto, moneda, periodo_inicio, periodo_fin)
            VALUES (?, ?, ?, ?, ?::numeric, ?, ?::date, ?::date)
            """)) {
            st.setObject(1, id);
            st.setObject(2, USUARIO);
            st.setString(3, tipo);
            st.setString(4, estado);
            st.setString(5, monto);
            st.setString(6, monto == null ? null : "USD");
            st.setString(7, inicio);
            st.setString(8, fin);
            st.executeUpdate();
        }
        return id;
    }

    private static List<RolAprobador> roles(UUID solicitud) throws Exception {
        return pasos(solicitud).stream().map(PasoAprobacion::rolAprobador).toList();
    }

    private static List<PasoAprobacion> pasos(UUID solicitud) throws Exception {
        try (var c = conexion(); var st = c.prepareStatement("SELECT pasos FROM solicitud WHERE id = ?")) {
            st.setObject(1, solicitud);
            try (var fila = st.executeQuery()) {
                fila.next();
                return new ObjectMapper().readValue(fila.getString(1), new TypeReference<>() { });
            }
        }
    }

    private static Connection conexion() throws SQLException {
        return DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
    }
}
