package com.nicolas.flujos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class DecisionesIT extends PruebaApi {

    private static final String APROBAR = "{\"tipo\":\"APROBAR\",\"comentario\":\"Visto bueno\"}";
    private static final String COMPRA_GRANDE = "{\"tipo\":\"COMPRA\",\"monto\":{\"valor\":20000,\"moneda\":\"USD\"}}";
    private static final String COMPRA_PEQUENA = "{\"tipo\":\"COMPRA\",\"monto\":{\"valor\":500,\"moneda\":\"USD\"}}";

    @Test
    void aprobarElUnicoPasoDejaLaSolicitudAprobada() throws Exception {
        var jefe = crearAprobador();
        var id = enviada(crearUsuarioConJefe(jefe), COMPRA_PEQUENA);

        decidir(id, jefe, APROBAR)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("APROBADA"))
            .andExpect(jsonPath("$.aprobadorActualId").doesNotExist())
            .andExpect(jsonPath("$.fechaLimitePaso").doesNotExist());
    }

    @Test
    void unaCompraGrandeRecorreJefeGerenteYDirector() throws Exception {
        crearConCargo("GERENTE");
        crearConCargo("DIRECTOR");
        var jefe = crearAprobador();
        var ana = crearUsuarioConJefe(jefe);
        var id = enviada(ana, COMPRA_GRANDE);

        var gerente = siguienteAprobador(decidir(id, jefe, APROBAR)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.pasoActual").value(2)));
        var director = siguienteAprobador(decidir(id, gerente, APROBAR)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pasoActual").value(3)));
        decidir(id, director, APROBAR)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("APROBADA"));

        var filas = jdbc.queryForList("SELECT paso FROM aprobacion WHERE solicitud_id = ? ORDER BY paso",
            Integer.class, UUID.fromString(id));
        assertThat(filas).containsExactly(1, 2, 3);
        auditoria(id, ana, "EMPLEADO")
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].accion").value("ENVIADA"))
            .andExpect(jsonPath("$[1].accion").value("APROBADA"))
            .andExpect(jsonPath("$[1].estadoNuevo").value("PENDIENTE"))
            .andExpect(jsonPath("$[1].detalle.comentario").value("Visto bueno"))
            .andExpect(jsonPath("$[3].estadoNuevo").value("APROBADA"));
    }

    @Test
    void rechazarExigeMotivoYNoTocaLaSolicitudSiFalta() throws Exception {
        var jefe = crearAprobador();
        var id = enviada(crearUsuarioConJefe(jefe), COMPRA_PEQUENA);

        decidir(id, jefe, "{\"tipo\":\"RECHAZAR\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentType(PROBLEM_JSON));

        consultar(id, jefe, "APROBADOR").andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void rechazarConMotivoTerminaLaSolicitudYQuedaEnLaAuditoria() throws Exception {
        var jefe = crearAprobador();
        var ana = crearUsuarioConJefe(jefe);
        var id = enviada(ana, COMPRA_GRANDE);

        decidir(id, jefe, "{\"tipo\":\"RECHAZAR\",\"comentario\":\"Sin presupuesto\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("RECHAZADA"));

        auditoria(id, ana, "EMPLEADO")
            .andExpect(jsonPath("$[1].accion").value("RECHAZADA"))
            .andExpect(jsonPath("$[1].actorId").value(jefe.toString()))
            .andExpect(jsonPath("$[1].detalle.comentario").value("Sin presupuesto"));
    }

    @Test
    void unaSolicitudYaDecididaDevuelve409ConEstadoActual() throws Exception {
        var jefe = crearAprobador();
        var id = enviada(crearUsuarioConJefe(jefe), COMPRA_PEQUENA);
        decidir(id, jefe, APROBAR);

        decidir(id, jefe, APROBAR)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.estadoActual").value("APROBADA"));
    }

    @Test
    void unBorradorNoSePuedeDecidir() throws Exception {
        var ana = crearUsuario();
        var id = idDe(crear(ana, COMPRA_PEQUENA));

        mvc.perform(post(RUTA + "/" + id + "/decisiones")
                .header(HttpHeaders.AUTHORIZATION, bearer(ana, "APROBADOR"))
                .contentType(MediaType.APPLICATION_JSON).content(APROBAR))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.estadoActual").value("BORRADOR"));
    }

    @Test
    void aprobacionesSimultaneasDelMismoPasoSoloGanaUna() throws Exception {
        var jefe = crearAprobador();
        var id = enviada(crearUsuarioConJefe(jefe), COMPRA_PEQUENA);
        var largada = new CountDownLatch(1);
        var codigos = new ArrayList<Integer>();

        try (var hilos = Executors.newFixedThreadPool(5)) {
            var resultados = new ArrayList<Future<Integer>>();
            for (int i = 0; i < 5; i++) {
                resultados.add(hilos.submit(() -> {
                    largada.await();
                    return decidir(id, jefe, APROBAR).andReturn().getResponse().getStatus();
                }));
            }
            largada.countDown();
            for (var resultado : resultados) {
                codigos.add(resultado.get());
            }
        }

        assertThat(codigos).containsOnly(200, 409).filteredOn(codigo -> codigo == 200).hasSize(1);
        var decisiones = jdbc.queryForObject("SELECT count(*) FROM aprobacion WHERE solicitud_id = ?",
            Long.class, UUID.fromString(id));
        var registros = jdbc.queryForObject("SELECT count(*) FROM registro_auditoria WHERE solicitud_id = ? AND accion = 'APROBADA'",
            Long.class, UUID.fromString(id));
        assertThat(decisiones).isEqualTo(1);
        assertThat(registros).isEqualTo(1);
    }

    @Test
    void elJefeSigueViendoLaSolicitudDespuesDeDecidir() throws Exception {
        crearConCargo("GERENTE");
        var jefe = crearAprobador();
        var id = enviada(crearUsuarioConJefe(jefe), COMPRA_GRANDE);
        decidir(id, jefe, APROBAR);

        consultar(id, jefe, "APROBADOR").andExpect(status().isOk());
        auditoria(id, jefe, "APROBADOR").andExpect(status().isOk());
    }

    @Test
    void unAprobadorQueYaDecidioNoPuedeDecidirElPasoSiguiente() throws Exception {
        crearConCargo("GERENTE");
        var jefe = crearAprobador();
        var id = enviada(crearUsuarioConJefe(jefe), COMPRA_GRANDE);
        decidir(id, jefe, APROBAR);

        decidir(id, jefe, APROBAR).andExpect(status().isForbidden());
    }

    @Test
    void unAprobadorAjenoRecibe404() throws Exception {
        var id = enviada(crearUsuarioConJefe(crearAprobador()), COMPRA_PEQUENA);

        decidir(id, crearAprobador(), APROBAR).andExpect(status().isNotFound());
    }

    @Test
    void elSolicitanteConRolAprobadorNoPuedeAprobarSuSolicitud() throws Exception {
        var ana = crearUsuarioConJefe(crearAprobador());
        var id = enviada(ana, COMPRA_PEQUENA);

        decidir(id, ana, APROBAR).andExpect(status().isForbidden());
    }

    @Test
    void sinRolAprobadorDevuelve403() throws Exception {
        var jefe = crearAprobador();
        var id = enviada(crearUsuarioConJefe(jefe), COMPRA_PEQUENA);

        mvc.perform(post(RUTA + "/" + id + "/decisiones")
                .header(HttpHeaders.AUTHORIZATION, bearer(jefe, "EMPLEADO"))
                .contentType(MediaType.APPLICATION_JSON).content(APROBAR))
            .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(post(RUTA + "/" + UUID.randomUUID() + "/decisiones")
                .contentType(MediaType.APPLICATION_JSON).content(APROBAR))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void cuerpoInvalidoDevuelve400() throws Exception {
        var jefe = crearAprobador();
        var id = enviada(crearUsuarioConJefe(jefe), COMPRA_PEQUENA);

        decidir(id, jefe, "{\"tipo\":\"QUIZAS\"}").andExpect(status().isBadRequest());
        decidir(id, jefe, "{}").andExpect(status().isBadRequest());
        decidir(id, jefe, "{\"tipo\":\"APROBAR\",\"comentario\":\"%s\"}".formatted("x".repeat(501)))
            .andExpect(status().isBadRequest());
    }

    private String enviada(UUID solicitante, String cuerpo) throws Exception {
        var id = idDe(crear(solicitante, cuerpo));
        accion(id, "envio", solicitante, "EMPLEADO").andExpect(status().isOk());
        return id;
    }

    private ResultActions decidir(String id, UUID aprobador, String cuerpo) throws Exception {
        return mvc.perform(post(RUTA + "/" + id + "/decisiones")
            .header(HttpHeaders.AUTHORIZATION, bearer(aprobador, "APROBADOR"))
            .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private UUID siguienteAprobador(ResultActions resultado) throws Exception {
        var cuerpo = mapper.readTree(resultado.andReturn().getResponse().getContentAsString());
        return UUID.fromString(cuerpo.get("aprobadorActualId").asText());
    }

    private void crearConCargo(String cargo) {
        var id = crearAprobador();
        jdbc.update("UPDATE usuario SET cargo = ? WHERE id = ?", cargo, id);
    }
}
