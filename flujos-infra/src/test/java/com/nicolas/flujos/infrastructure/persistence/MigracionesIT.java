package com.nicolas.flujos.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MigracionesIT {

    private static final String PASSWORD_APP = "clave-de-prueba";
    private static final String INSERT_AUDITORIA =
        "INSERT INTO registro_auditoria (solicitud_id, accion) VALUES (gen_random_uuid(), 'ENVIADA')";

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void migrar() {
        Flyway.configure()
            .dataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())
            .placeholders(Map.of("appPassword", PASSWORD_APP))
            .load()
            .migrate();
    }

    @Test
    void laAplicacionPuedeInsertarYLeerAuditoria() throws SQLException {
        try (Connection c = conexionApp(); Statement st = c.createStatement()) {
            assertThat(st.executeUpdate(INSERT_AUDITORIA)).isEqualTo(1);
            assertThat(st.executeQuery("SELECT 1 FROM registro_auditoria").next()).isTrue();
        }
    }

    @Test
    void laAplicacionNoPuedeModificarNiBorrarAuditoria() throws SQLException {
        try (Connection c = conexionApp(); Statement st = c.createStatement()) {
            assertThatThrownBy(() -> st.executeUpdate("UPDATE registro_auditoria SET accion = 'X'"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("permission denied");
            assertThatThrownBy(() -> st.executeUpdate("DELETE FROM registro_auditoria"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("permission denied");
        }
    }

    @Test
    void laAplicacionNoPuedeAlterarElEsquema() throws SQLException {
        try (Connection c = conexionApp(); Statement st = c.createStatement()) {
            assertThatThrownBy(() -> st.executeUpdate("DROP TABLE solicitud"))
                .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void unUsuarioNoPuedeSerSuPropioJefe() throws SQLException {
        var id = UUID.randomUUID();
        var insert = """
            INSERT INTO usuario (id, email, nombre, password_hash, jefe_id)
            VALUES ('%s', 'x@flujos.test', 'X', 'hash', '%s')
            """.formatted(id, id);

        try (Connection c = conexionApp(); Statement st = c.createStatement()) {
            assertThatThrownBy(() -> st.executeUpdate(insert))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("chk_jefe_distinto");
        }
    }

    private static Connection conexionApp() throws SQLException {
        return DriverManager.getConnection(pg.getJdbcUrl(), "flujos_app", PASSWORD_APP);
    }
}
