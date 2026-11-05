package com.nicolas.flujos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AutenticacionIT.ControladorDePrueba.class)
class AutenticacionIT extends IntegracionBase {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String CLAVE = "clave-de-prueba";
    private static final String HASH = new BCryptPasswordEncoder(4).encode(CLAVE);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    @Test
    void loginCorrectoEntregaUnTokenQueAbreRutasProtegidas() throws Exception {
        var email = crearUsuario("ADMIN");

        var tokens = login(email, CLAVE)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andExpect(jsonPath("$.refreshToken", not(emptyString())));

        mvc.perform(get("/prueba/admin").header(HttpHeaders.AUTHORIZATION, bearer(leer(tokens).get("accessToken").asText())))
            .andExpect(status().isOk());
    }

    @Test
    void contrasenaIncorrectaYEmailInexistenteResponden401Identicos() throws Exception {
        var email = crearUsuario("EMPLEADO");

        var mala = login(email, "otra-clave")
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(PROBLEM_JSON))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
            .andReturn().getResponse().getContentAsString();
        var inexistente = login("nadie@flujos.test", "otra-clave")
            .andExpect(status().isUnauthorized())
            .andReturn().getResponse().getContentAsString();

        // el traceId es de cada petición; todo lo demás tiene que ser idéntico
        assertThat(sinTraceId(mala)).isEqualTo(sinTraceId(inexistente));
    }

    @Test
    void usuarioInactivoNoPuedeEntrar() throws Exception {
        var email = crearUsuario("EMPLEADO");
        jdbc.update("UPDATE usuario SET activo = false WHERE email = ?", email);

        login(email, CLAVE).andExpect(status().isUnauthorized());
    }

    @Test
    void loginConCamposInvalidosDevuelve400ConErrores() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"no-es-email\",\"password\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(PROBLEM_JSON))
            .andExpect(jsonPath("$.errores.length()").value(2));
    }

    @Test
    void sinTokenDevuelve401ConProblemJson() throws Exception {
        mvc.perform(get("/prueba/admin"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(PROBLEM_JSON))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
            .andExpect(jsonPath("$.instance").value("/prueba/admin"));
    }

    @Test
    void tokenVencidoDevuelve401() throws Exception {
        mvc.perform(get("/prueba/admin").header(HttpHeaders.AUTHORIZATION, bearer(TokenDePrueba.vencido())))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFirmadoConOtraClaveDevuelve401() throws Exception {
        mvc.perform(get("/prueba/admin").header(HttpHeaders.AUTHORIZATION, bearer(TokenDePrueba.firmadoConOtraClave())))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rolInsuficienteEnPreAuthorizeDevuelve403ConProblemJson() throws Exception {
        mvc.perform(get("/prueba/admin").header(HttpHeaders.AUTHORIZATION, bearer(TokenDePrueba.conRoles("EMPLEADO"))))
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://flujos.api/errores/acceso-denegado"));
    }

    @Test
    void soloHealthEsPublicoEnActuator() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotaElTokenYElNuevoSigueFuncionando() throws Exception {
        var inicial = leer(login(crearUsuario("EMPLEADO"), CLAVE));

        var renovado = leer(refresh(inicial.get("refreshToken").asText()).andExpect(status().isOk()));

        assertThat(renovado.get("refreshToken").asText()).isNotEqualTo(inicial.get("refreshToken").asText());
        refresh(renovado.get("refreshToken").asText()).andExpect(status().isOk());
    }

    @Test
    void reutilizarUnRefreshTokenRevocaTambienElNuevo() throws Exception {
        var inicial = leer(login(crearUsuario("EMPLEADO"), CLAVE)).get("refreshToken").asText();
        var nuevo = leer(refresh(inicial)).get("refreshToken").asText();

        refresh(inicial).andExpect(status().isUnauthorized());

        refresh(nuevo).andExpect(status().isUnauthorized());
    }

    @Test
    void dosRenovacionesSimultaneasNoPuedenUsarElMismoToken() throws Exception {
        var token = leer(login(crearUsuario("EMPLEADO"), CLAVE)).get("refreshToken").asText();
        var intentos = 5;
        var largada = new CountDownLatch(1);

        try (var hilos = Executors.newFixedThreadPool(intentos)) {
            var resultados = new ArrayList<Future<Integer>>();
            for (int i = 0; i < intentos; i++) {
                resultados.add(hilos.submit(() -> {
                    largada.await();
                    return refresh(token).andReturn().getResponse().getStatus();
                }));
            }
            largada.countDown();

            var exitosas = 0;
            for (var resultado : resultados) {
                exitosas += resultado.get() == 200 ? 1 : 0;
            }
            assertThat(exitosas).isEqualTo(1);
        }
    }

    @Test
    void refreshVencidoOInventadoDevuelve401() throws Exception {
        refresh("token-inventado").andExpect(status().isUnauthorized());

        var email = crearUsuario("EMPLEADO");
        var token = leer(login(email, CLAVE)).get("refreshToken").asText();
        jdbc.update("""
            UPDATE refresh_token SET expira_en = now() - interval '1 minute'
            WHERE usuario_id = (SELECT id FROM usuario WHERE email = ?)
            """, email);

        refresh(token).andExpect(status().isUnauthorized());
    }

    @Test
    void elHashEsLoUnicoQueSeGuardaDelRefreshToken() throws Exception {
        var email = crearUsuario("EMPLEADO");
        var token = leer(login(email, CLAVE)).get("refreshToken").asText();

        var guardado = jdbc.queryForObject("""
            SELECT token_hash FROM refresh_token
            WHERE usuario_id = (SELECT id FROM usuario WHERE email = ?)
            """, String.class, email);

        assertThat(guardado).hasSize(64).isNotEqualTo(token);
    }

    @Test
    void logoutInvalidaElRefreshToken() throws Exception {
        var tokens = leer(login(crearUsuario("EMPLEADO"), CLAVE));
        var refreshToken = tokens.get("refreshToken").asText();

        mvc.perform(post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokens.get("accessToken").asText()))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoRefresh(refreshToken)))
            .andExpect(status().isNoContent());

        refresh(refreshToken).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutExigeAutenticacion() throws Exception {
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content(cuerpoRefresh("x")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutNoPuedeInvalidarElTokenDeOtroUsuario() throws Exception {
        var ajeno = leer(login(crearUsuario("EMPLEADO"), CLAVE)).get("refreshToken").asText();
        var mio = leer(login(crearUsuario("EMPLEADO"), CLAVE)).get("accessToken").asText();

        mvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(mio))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoRefresh(ajeno)))
            .andExpect(status().isNoContent());

        refresh(ajeno).andExpect(status().isOk());
    }

    private String crearUsuario(String rol) {
        var id = UUID.randomUUID();
        var email = id + "@flujos.test";
        jdbc.update("INSERT INTO usuario (id, email, nombre, password_hash) VALUES (?, ?, 'Prueba', ?)", id, email, HASH);
        jdbc.update("INSERT INTO usuario_rol (usuario_id, rol) VALUES (?, ?)", id, rol);
        return email;
    }

    private JsonNode sinTraceId(String cuerpo) throws Exception {
        var json = (ObjectNode) mapper.readTree(cuerpo);
        json.remove("traceId");
        return json;
    }

    private ResultActions login(String email, String password) throws Exception {
        var cuerpo = mapper.createObjectNode().put("email", email).put("password", password);
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo.toString()));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(cuerpoRefresh(refreshToken)));
    }

    private String cuerpoRefresh(String refreshToken) {
        return mapper.createObjectNode().put("refreshToken", refreshToken).toString();
    }

    private JsonNode leer(ResultActions resultado) throws Exception {
        return mapper.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @RestController
    static class ControladorDePrueba {

        @GetMapping("/prueba/admin")
        @PreAuthorize("hasRole('ADMIN')")
        String soloAdmin() {
            return "ok";
        }
    }
}
