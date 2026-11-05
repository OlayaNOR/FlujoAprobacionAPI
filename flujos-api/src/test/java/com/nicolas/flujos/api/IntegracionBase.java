package com.nicolas.flujos.api;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/** Un solo contenedor para todas las pruebas: Testcontainers lo elimina al terminar la JVM. */
abstract class IntegracionBase {

    private static final String PASSWORD_APP = "clave-app-de-prueba";

    static final String SECRETO_JWT = "secreto-de-prueba-con-mas-de-32-bytes!!";

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
        r.add("app.seguridad.jwt.secreto", () -> SECRETO_JWT);
        r.add("app.jobs.habilitados", () -> "false");
    }
}
