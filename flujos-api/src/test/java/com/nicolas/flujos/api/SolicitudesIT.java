package com.nicolas.flujos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class SolicitudesIT extends IntegracionBase {

    private static final String RUTA = "/api/v1/solicitudes";
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String COMPRA = """
        {"tipo":"COMPRA","monto":{"valor":1250.50,"moneda":"USD"},"motivo":"Licencias"}
        """;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    @Test
    void crearDevuelve201ConLocationYEstadoBorrador() throws Exception {
        var ana = crearUsuario();

        crear(ana, COMPRA)
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.LOCATION, startsWith(RUTA + "/")))
            .andExpect(jsonPath("$.estado").value("BORRADOR"))
            .andExpect(jsonPath("$.tipo").value("COMPRA"))
            .andExpect(jsonPath("$.monto.valor").value(1250.50))
            .andExpect(jsonPath("$.monto.moneda").value("USD"))
            .andExpect(jsonPath("$.solicitanteId").value(ana.toString()))
            .andExpect(jsonPath("$.periodo").doesNotExist());
    }

    @Test
    void lasFechasDeRespuestaVanEnIso8601Utc() throws Exception {
        crear(crearUsuario(), COMPRA)
            .andExpect(jsonPath("$.creadaEn", endsWith("Z")));
    }

    @Test
    void crearVacacionesConPeriodoValido() throws Exception {
        var hoy = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        var cuerpo = """
            {"tipo":"VACACIONES","periodo":{"inicio":"%s","fin":"%s"}}
            """.formatted(hoy, hoy.plusDays(4));

        crear(crearUsuario(), cuerpo)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.periodo.inicio").value(hoy.toString()))
            .andExpect(jsonPath("$.monto").doesNotExist());
    }

    @Test
    void sinRolEmpleadoNoPuedeCrear() throws Exception {
        mvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID(), "APROBADOR"))
                .contentType(MediaType.APPLICATION_JSON).content(COMPRA))
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(PROBLEM_JSON));
    }

    @Test
    void sinTokenNoPuedeCrear() throws Exception {
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(COMPRA))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void errorDeFormatoDevuelve400ConLosCampos() throws Exception {
        var cuerpo = """
            {"tipo":"COMPRA","monto":{"valor":-5,"moneda":"usd"},"motivo":"%s"}
            """.formatted("x".repeat(501));

        crear(crearUsuario(), cuerpo)
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(PROBLEM_JSON))
            .andExpect(jsonPath("$.errores.length()").value(3));
    }

    @Test
    void tipoDesconocidoDevuelve400() throws Exception {
        crear(crearUsuario(), "{\"tipo\":\"PRESTAMO\"}").andExpect(status().isBadRequest());
    }

    @Test
    void montoQueNoCabeEnLaBaseDevuelve400() throws Exception {
        crear(crearUsuario(), "{\"tipo\":\"COMPRA\",\"monto\":{\"valor\":1000000000000,\"moneda\":\"USD\"}}")
            .andExpect(status().isBadRequest());
    }

    @Test
    void reglasDeNegocioDevuelven422() throws Exception {
        var ana = crearUsuario();
        var manana = LocalDate.now(ZoneOffset.UTC).plusDays(1);

        crear(ana, "{\"tipo\":\"COMPRA\"}").andExpect(status().isUnprocessableEntity());
        crear(ana, "{\"tipo\":\"COMPRA\",\"monto\":{\"valor\":10,\"moneda\":\"COP\"}}").andExpect(status().isUnprocessableEntity());
        crear(ana, "{\"tipo\":\"COMPRA\",\"monto\":{\"valor\":10,\"moneda\":\"ZZZ\"}}").andExpect(status().isUnprocessableEntity());
        crear(ana, """
            {"tipo":"VACACIONES","periodo":{"inicio":"%s","fin":"%s"}}
            """.formatted(manana, manana.minusDays(1))).andExpect(status().isUnprocessableEntity());
        crear(ana, """
            {"tipo":"COMPRA","monto":{"valor":10,"moneda":"USD"},"periodo":{"inicio":"%s","fin":"%s"}}
            """.formatted(manana, manana)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void elDuenoConsultaSuSolicitud() throws Exception {
        var ana = crearUsuario();
        var id = idDe(crear(ana, COMPRA));

        consultar(id, ana, "EMPLEADO")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.motivo").value("Licencias"));
    }

    @Test
    void otroEmpleadoRecibe404IgualQueSiNoExistiera() throws Exception {
        var id = idDe(crear(crearUsuario(), COMPRA));
        var luis = crearUsuario();

        var ajena = consultar(id, luis, "EMPLEADO").andExpect(status().isNotFound())
            .andReturn().getResponse().getContentAsString();
        var inexistente = consultar(UUID.randomUUID().toString(), luis, "EMPLEADO").andExpect(status().isNotFound())
            .andReturn().getResponse().getContentAsString();

        var detalle = "\"detail\":\"Solicitud no encontrada\"";
        assertThat(ajena).contains(detalle).doesNotContain("Licencias");
        assertThat(inexistente).contains(detalle);
    }

    @Test
    void elAdminPuedeConsultarCualquierSolicitud() throws Exception {
        var id = idDe(crear(crearUsuario(), COMPRA));

        consultar(id, UUID.randomUUID(), "ADMIN").andExpect(status().isOk());
    }

    @Test
    void idMalFormadoDevuelve400() throws Exception {
        consultar("no-es-uuid", UUID.randomUUID(), "EMPLEADO")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://flujos.api/errores/parametro-invalido"));
    }

    @Test
    void elDuenoEditaSuBorrador() throws Exception {
        var ana = crearUsuario();
        var id = idDe(crear(ana, COMPRA));

        editar(id, ana, "EMPLEADO", "{\"monto\":{\"valor\":99.90,\"moneda\":\"USD\"},\"motivo\":\"Otro\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.monto.valor").value(99.90))
            .andExpect(jsonPath("$.motivo").value("Otro"));

        consultar(id, ana, "EMPLEADO").andExpect(jsonPath("$.monto.valor").value(99.90));
    }

    @Test
    void otroEmpleadoNoPuedeEditarYRecibe404() throws Exception {
        var id = idDe(crear(crearUsuario(), COMPRA));

        editar(id, crearUsuario(), "EMPLEADO", "{\"monto\":{\"valor\":1,\"moneda\":\"USD\"}}")
            .andExpect(status().isNotFound());
    }

    @Test
    void elAdminVePeroNoEditaYRecibe403() throws Exception {
        var id = idDe(crear(crearUsuario(), COMPRA));

        editar(id, UUID.randomUUID(), "ADMIN", "{\"monto\":{\"valor\":1,\"moneda\":\"USD\"}}")
            .andExpect(status().isForbidden());
    }

    @Test
    void editarUnaSolicitudEnviadaDevuelve409ConEstadoActual() throws Exception {
        var ana = crearUsuario();
        var id = idDe(crear(ana, COMPRA));
        jdbc.update("UPDATE solicitud SET estado = 'PENDIENTE' WHERE id = ?", UUID.fromString(id));

        editar(id, ana, "EMPLEADO", "{\"monto\":{\"valor\":1,\"moneda\":\"USD\"}}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.estadoActual").value("PENDIENTE"));
    }

    private ResultActions crear(UUID usuario, String cuerpo) throws Exception {
        return mvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(usuario, "EMPLEADO"))
            .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions consultar(String id, UUID usuario, String rol) throws Exception {
        return mvc.perform(get(RUTA + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(usuario, rol)));
    }

    private ResultActions editar(String id, UUID usuario, String rol, String cuerpo) throws Exception {
        return mvc.perform(put(RUTA + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(usuario, rol))
            .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private String idDe(ResultActions resultado) throws Exception {
        return mapper.readTree(resultado.andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    private UUID crearUsuario() {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario (id, email, nombre, password_hash) VALUES (?, ?, 'Prueba', 'hash')",
            id, id + "@flujos.test");
        return id;
    }

    private static String bearer(UUID usuario, String... roles) {
        return "Bearer " + TokenDePrueba.para(usuario, roles);
    }
}
