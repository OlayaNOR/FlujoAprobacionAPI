package com.nicolas.flujos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nicolas.flujos.application.port.in.EscalarVencidasUseCase;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EscalamientoIT extends PruebaApi {

    private static final String COMPRA_MEDIANA = "{\"tipo\":\"COMPRA\",\"monto\":{\"valor\":5000,\"moneda\":\"USD\"}}";
    private static final String COMPRA_PEQUENA = "{\"tipo\":\"COMPRA\",\"monto\":{\"valor\":500,\"moneda\":\"USD\"}}";

    @Autowired EscalarVencidasUseCase escalar;

    @Test
    void unaSolicitudVencidaPasaAlSiguienteAprobador() throws Exception {
        crearConCargo("GERENTE");
        var jefe = crearAprobador();
        var ana = crearUsuarioConJefe(jefe);
        var id = enviada(ana, COMPRA_MEDIANA);
        vencer(id);
        var antes = Instant.now();

        assertThat(escalar.ejecutar().escaladas()).isPositive();

        var gerente = siguienteAprobador(consultar(id, ana, "EMPLEADO")
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.pasoActual").value(2)));
        assertThat(jdbc.queryForObject("SELECT cargo FROM usuario WHERE id = ?", String.class, gerente)).isEqualTo("GERENTE");
        var limite = jdbc.queryForObject("SELECT fecha_limite_paso FROM solicitud WHERE id = ?", Timestamp.class,
            UUID.fromString(id)).toInstant();
        assertThat(limite).isAfter(antes.plus(Duration.ofHours(47)));

        auditoria(id, ana, "EMPLEADO")
            .andExpect(jsonPath("$[-1:].accion").value("ESCALADA"))
            .andExpect(jsonPath("$[-1:].detalle.pasoAnterior").value(1));
        assertThat(eventos(id)).containsExactly("SolicitudEnviada", "SolicitudEscalada");

        decidir(id, gerente, "{\"tipo\":\"APROBAR\"}").andExpect(jsonPath("$.estado").value("APROBADA"));
    }

    @Test
    void elJefeQueNoDecidioDejaDeVerla() throws Exception {
        crearConCargo("GERENTE");
        var jefe = crearAprobador();
        var id = enviada(crearUsuarioConJefe(jefe), COMPRA_MEDIANA);
        vencer(id);

        escalar.ejecutar();

        consultar(id, jefe, "APROBADOR").andExpect(status().isNotFound());
        decidir(id, jefe, "{\"tipo\":\"APROBAR\"}").andExpect(status().isNotFound());
    }

    @Test
    void enElUltimoPasoAvisaAlAdminUnaSolaVez() throws Exception {
        var jefe = crearAprobador();
        var ana = crearUsuarioConJefe(jefe);
        var id = enviada(ana, COMPRA_PEQUENA);
        vencer(id);

        escalar.ejecutar();
        escalar.ejecutar();

        consultar(id, ana, "EMPLEADO")
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.pasoActual").value(1))
            .andExpect(jsonPath("$.aprobadorActualId").value(jefe.toString()))
            .andExpect(jsonPath("$.fechaLimitePaso").doesNotExist());
        var avisos = jdbc.queryForObject(
            "SELECT count(*) FROM registro_auditoria WHERE solicitud_id = ? AND accion = 'ESCALADA_ADMIN'",
            Long.class, UUID.fromString(id));
        assertThat(avisos).isEqualTo(1);
        var payload = jdbc.queryForObject(
            "SELECT payload->>'notificarAdmin' FROM outbox WHERE agregado_id = ? AND tipo_evento = 'SolicitudEscalada'",
            String.class, UUID.fromString(id));
        assertThat(payload).isEqualTo("true");

        decidir(id, jefe, "{\"tipo\":\"APROBAR\"}").andExpect(jsonPath("$.estado").value("APROBADA"));
    }

    @Test
    void unaSolicitudQueNoVencioNoSeToca() throws Exception {
        var ana = crearUsuarioConJefe(crearAprobador());
        var id = enviada(ana, COMPRA_MEDIANA);

        escalar.ejecutar();

        consultar(id, ana, "EMPLEADO").andExpect(jsonPath("$.pasoActual").value(1));
        assertThat(eventos(id)).containsExactly("SolicitudEnviada");
    }

    private void vencer(String id) {
        jdbc.update("UPDATE solicitud SET fecha_limite_paso = now() - interval '1 minute' WHERE id = ?",
            UUID.fromString(id));
    }

    private List<String> eventos(String id) {
        return jdbc.queryForList("SELECT tipo_evento FROM outbox WHERE agregado_id = ? ORDER BY id",
            String.class, UUID.fromString(id));
    }
}
