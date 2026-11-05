package com.nicolas.flujos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

@TestPropertySource(properties = {
    "app.seguridad.login.intentos-por-minuto-ip=3",
    "app.seguridad.login.fallos-por-hora-email=2"
})
class LimiteLoginIT extends PruebaApi {

    private static final String CLAVE = "clave-de-prueba-larga";
    private static final AtomicInteger IPS = new AtomicInteger();

    @Test
    void elCuartoIntentoDesdeLaMismaIpDevuelve429() throws Exception {
        var ip = ipNueva();
        for (int i = 0; i < 3; i++) {
            login(ip, emailNuevo(), "mala").andExpect(status().isUnauthorized());
        }

        var respuesta = login(ip, emailNuevo(), "mala")
            .andExpect(status().isTooManyRequests())
            .andExpect(content().contentType(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://flujos.api/errores/demasiados-intentos"))
            .andReturn().getResponse();

        assertThat(Long.parseLong(respuesta.getHeader("Retry-After"))).isBetween(1L, 60L);
        login(ipNueva(), emailNuevo(), "mala").andExpect(status().isUnauthorized());
    }

    @Test
    void elRefreshNoCuentaParaElLimite() throws Exception {
        var ip = ipNueva();
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/v1/auth/refresh").with(r -> desde(r, ip))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"no-existe\"}"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void despuesDeDosFallosElEmailQuedaBloqueadoAunqueLaClaveSeaCorrecta() throws Exception {
        var email = crearCuenta();
        login(ipNueva(), email, "mala").andExpect(status().isUnauthorized());
        login(ipNueva(), email, "mala").andExpect(status().isUnauthorized());

        var respuesta = login(ipNueva(), email, CLAVE)
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andReturn().getResponse();

        // dos por hora: recuperar un intento toma media hora
        assertThat(Long.parseLong(respuesta.getHeader("Retry-After"))).isBetween(1700L, 1800L);
        login(ipNueva(), emailNuevo(), "mala").andExpect(status().isUnauthorized());
    }

    @Test
    void unLoginCorrectoReiniciaLosFallos() throws Exception {
        var email = crearCuenta();

        login(ipNueva(), email, "mala").andExpect(status().isUnauthorized());
        login(ipNueva(), email, CLAVE).andExpect(status().isOk());
        login(ipNueva(), email, "mala").andExpect(status().isUnauthorized());
        login(ipNueva(), email, CLAVE).andExpect(status().isOk());
    }

    @Test
    void elBloqueoNoDependeDeMayusculas() throws Exception {
        var email = crearCuenta();
        login(ipNueva(), email.toUpperCase(), "mala").andExpect(status().isUnauthorized());
        login(ipNueva(), Character.toUpperCase(email.charAt(0)) + email.substring(1), "mala")
            .andExpect(status().isUnauthorized());

        login(ipNueva(), email, CLAVE).andExpect(status().isTooManyRequests());
    }

    private ResultActions login(String ip, String email, String password) throws Exception {
        var cuerpo = mapper.createObjectNode().put("email", email).put("password", password);
        return mvc.perform(post("/api/v1/auth/login").with(r -> desde(r, ip))
            .contentType(MediaType.APPLICATION_JSON).content(cuerpo.toString()));
    }

    private String crearCuenta() {
        var email = emailNuevo();
        jdbc.update("INSERT INTO usuario (id, email, nombre, password_hash) VALUES (?, ?, 'Prueba', ?)",
            UUID.randomUUID(), email, new BCryptPasswordEncoder().encode(CLAVE));
        jdbc.update("INSERT INTO usuario_rol (usuario_id, rol) SELECT id, 'EMPLEADO' FROM usuario WHERE email = ?", email);
        return email;
    }

    private static MockHttpServletRequest desde(MockHttpServletRequest request, String ip) {
        request.setRemoteAddr(ip);
        return request;
    }

    private static String ipNueva() {
        var n = IPS.incrementAndGet();
        return "10.0.%d.%d".formatted(n / 250, n % 250 + 1);
    }

    private static String emailNuevo() {
        return UUID.randomUUID() + "@flujos.test";
    }
}
