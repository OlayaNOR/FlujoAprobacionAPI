package com.nicolas.flujos.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
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
abstract class PruebaApi extends IntegracionBase {

    static final String RUTA = "/api/v1/solicitudes";
    static final String PROBLEM_JSON = "application/problem+json";
    static final String COMPRA = """
        {"tipo":"COMPRA","monto":{"valor":1250.50,"moneda":"USD"},"motivo":"Licencias"}
        """;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    ResultActions crear(UUID usuario, String cuerpo) throws Exception {
        return mvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(usuario, "EMPLEADO"))
            .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    ResultActions consultar(String id, UUID usuario, String rol) throws Exception {
        return mvc.perform(get(RUTA + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(usuario, rol)));
    }

    ResultActions editar(String id, UUID usuario, String rol, String cuerpo) throws Exception {
        return mvc.perform(put(RUTA + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(usuario, rol))
            .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    ResultActions listar(UUID usuario, String rol, String consulta) throws Exception {
        return mvc.perform(get(RUTA + consulta).header(HttpHeaders.AUTHORIZATION, bearer(usuario, rol)));
    }

    ResultActions accion(String id, String accion, UUID usuario, String rol) throws Exception {
        return mvc.perform(post(RUTA + "/" + id + "/" + accion).header(HttpHeaders.AUTHORIZATION, bearer(usuario, rol)));
    }

    ResultActions auditoria(String id, UUID usuario, String rol) throws Exception {
        return mvc.perform(get(RUTA + "/" + id + "/auditoria").header(HttpHeaders.AUTHORIZATION, bearer(usuario, rol)));
    }

    String idDe(ResultActions resultado) throws Exception {
        return mapper.readTree(resultado.andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    UUID crearUsuario() {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario (id, email, nombre, password_hash) VALUES (?, ?, 'Prueba', 'hash')",
            id, id + "@flujos.test");
        return id;
    }

    UUID crearAprobador() {
        var id = crearUsuario();
        jdbc.update("INSERT INTO usuario_rol (usuario_id, rol) VALUES (?, 'APROBADOR')", id);
        return id;
    }

    UUID crearUsuarioConJefe(UUID jefe) {
        var id = crearUsuario();
        jdbc.update("UPDATE usuario SET jefe_id = ? WHERE id = ?", jefe, id);
        return id;
    }

    static String bearer(UUID usuario, String... roles) {
        return "Bearer " + TokenDePrueba.para(usuario, roles);
    }
}
