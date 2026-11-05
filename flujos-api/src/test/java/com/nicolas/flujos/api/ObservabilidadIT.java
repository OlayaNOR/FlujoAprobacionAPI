package com.nicolas.flujos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nicolas.flujos.infrastructure.metrics.SolicitudesPorEstado;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

// Sin esta anotación, las pruebas de Spring Boot apagan el tracing y no habría traceId.
@AutoConfigureObservability
class ObservabilidadIT extends PruebaApi {

    private static final String COMPRA_PEQUENA = "{\"tipo\":\"COMPRA\",\"monto\":{\"valor\":500,\"moneda\":\"USD\"}}";

    @Autowired MeterRegistry registro;
    @Autowired SolicitudesPorEstado porEstado;

    @Test
    void cuentaLasSolicitudesCreadasPeroNoLasRepeticionesIdempotentes() throws Exception {
        var antes = creadas("COMPRA");
        var ana = crearUsuario();

        crear(ana, COMPRA_PEQUENA).andExpect(status().isCreated());
        for (int i = 0; i < 2; i++) {
            mvc.perform(post(RUTA)
                    .header(HttpHeaders.AUTHORIZATION, bearer(ana, "EMPLEADO"))
                    .header("Idempotency-Key", "metricas-" + ana)
                    .contentType(MediaType.APPLICATION_JSON).content(COMPRA_PEQUENA))
                .andExpect(status().isCreated());
        }

        assertThat(creadas("COMPRA")).isEqualTo(antes + 2);
    }

    @Test
    void registraElTiempoDeCadaDecisionPorTipoYResultado() throws Exception {
        var aprobadas = decisiones("APROBADA");
        var rechazadas = decisiones("RECHAZADA");
        var jefe = crearAprobador();
        var ana = crearUsuarioConJefe(jefe);

        decidir(enviada(ana, COMPRA_PEQUENA), jefe, "{\"tipo\":\"APROBAR\"}").andExpect(status().isOk());
        decidir(enviada(ana, COMPRA_PEQUENA), jefe, "{\"tipo\":\"RECHAZAR\",\"comentario\":\"No\"}")
            .andExpect(status().isOk());

        assertThat(decisiones("APROBADA")).isEqualTo(aprobadas + 1);
        assertThat(decisiones("RECHAZADA")).isEqualTo(rechazadas + 1);
    }

    @Test
    void unaDecisionInvalidaNoSeRegistra() throws Exception {
        var antes = decisiones("RECHAZADA");
        var jefe = crearAprobador();
        var id = enviada(crearUsuarioConJefe(jefe), COMPRA_PEQUENA);

        decidir(id, jefe, "{\"tipo\":\"RECHAZAR\"}").andExpect(status().isUnprocessableEntity());

        assertThat(decisiones("RECHAZADA")).isEqualTo(antes);
    }

    @Test
    void elGaugePorEstadoReflejaLaBase() throws Exception {
        crear(crearUsuario(), COMPRA_PEQUENA).andExpect(status().isCreated());

        porEstado.actualizar();

        var enBase = jdbc.queryForObject("SELECT count(*) FROM solicitud WHERE estado = 'BORRADOR'", Long.class);
        var gauge = registro.get("solicitudes.por_estado").tag("estado", "BORRADOR").gauge().value();
        assertThat((long) gauge).isEqualTo(enBase);
        assertThat(registro.get("solicitudes.por_estado").tag("estado", "CERRADA").gauge()).isNotNull();
    }

    @Test
    void soloHealthEsPublicoYLasMetricasSonDeAdmin() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/metrics").header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID(), "EMPLEADO")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/metrics/solicitudes.por_estado")
                .header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID(), "ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void losErroresIncluyenElTraceIdDeLaPeticion() throws Exception {
        for (int i = 0; i < 5; i++) {
            consultar(UUID.randomUUID().toString(), crearUsuario(), "EMPLEADO")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").value(matchesPattern("[0-9a-f]{16,32}")));
        }
        mvc.perform(get(RUTA))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.traceId").exists());
    }

    private double creadas(String tipo) {
        var contador = registro.find("solicitudes.creadas").tag("tipo", tipo).counter();
        return contador == null ? 0 : contador.count();
    }

    private long decisiones(String resultado) {
        var timer = registro.find("aprobaciones.tiempo_respuesta").tag("tipo", "COMPRA").tag("resultado", resultado).timer();
        return timer == null ? 0 : timer.count();
    }
}
