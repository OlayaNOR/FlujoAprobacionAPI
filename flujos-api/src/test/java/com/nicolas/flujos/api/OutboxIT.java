package com.nicolas.flujos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.nicolas.flujos.infrastructure.messaging.RelevoOutbox;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OutboxIT extends PruebaApi {

    private static final String APROBAR = "{\"tipo\":\"APROBAR\"}";
    private static final String COMPRA_PEQUENA = "{\"tipo\":\"COMPRA\",\"monto\":{\"valor\":500,\"moneda\":\"USD\"}}";

    @Autowired RelevoOutbox relevo;

    @Test
    void enviarYAprobarDejanSusEventosEnElOutbox() throws Exception {
        var jefe = crearAprobador();
        var id = enviada(crearUsuarioConJefe(jefe), COMPRA_PEQUENA);
        decidir(id, jefe, APROBAR).andExpect(status().isOk());

        assertThat(tipos(id)).containsExactly("SolicitudEnviada", "SolicitudAprobada");
        var enviada = payload(id, "SolicitudEnviada");
        assertThat(enviada.get("solicitudId").asText()).isEqualTo(id);
        assertThat(enviada.get("aprobadorId").asText()).isEqualTo(jefe.toString());
        assertThat(enviada.has("ocurridoEn")).isTrue();
    }

    @Test
    void rechazarDejaSuEvento() throws Exception {
        var jefe = crearAprobador();
        var id = enviada(crearUsuarioConJefe(jefe), COMPRA_PEQUENA);

        decidir(id, jefe, "{\"tipo\":\"RECHAZAR\",\"comentario\":\"No\"}").andExpect(status().isOk());

        assertThat(tipos(id)).containsExactly("SolicitudEnviada", "SolicitudRechazada");
    }

    @Test
    void unaOperacionFallidaNoDejaEventos() throws Exception {
        var ana = crearUsuario();
        var id = idDe(crear(ana, COMPRA_PEQUENA));

        // sin jefe la asignación falla con 422 y la transacción se revierte entera
        accion(id, "envio", ana, "EMPLEADO").andExpect(status().isUnprocessableEntity());

        assertThat(tipos(id)).isEmpty();
    }

    @Test
    void elRelevoMarcaLosEventosComoPublicados() throws Exception {
        var id = enviada(crearUsuarioConJefe(crearAprobador()), COMPRA_PEQUENA);

        while (relevo.publicarPendientes() > 0) {
            // vacía también lo que dejaron otras pruebas
        }

        var sinPublicar = jdbc.queryForObject(
            "SELECT count(*) FROM outbox WHERE agregado_id = ? AND publicado_en IS NULL", Long.class, UUID.fromString(id));
        assertThat(sinPublicar).isZero();
        assertThat(relevo.publicarPendientes()).isZero();
    }

    @Test
    void dosRelevosSimultaneosNoPublicanElMismoEvento() throws Exception {
        var jefe = crearAprobador();
        for (int i = 0; i < 60; i++) {
            enviada(crearUsuarioConJefe(jefe), COMPRA_PEQUENA);
        }
        var pendientes = jdbc.queryForObject("SELECT count(*) FROM outbox WHERE publicado_en IS NULL", Long.class);
        var largada = new CountDownLatch(1);
        Callable<Integer> vaciar = () -> {
            largada.await();
            int total = 0;
            for (int n = relevo.publicarPendientes(); n > 0; n = relevo.publicarPendientes()) {
                total += n;
            }
            return total;
        };

        int publicados = 0;
        try (var hilos = Executors.newFixedThreadPool(2)) {
            var resultados = new ArrayList<Future<Integer>>();
            resultados.add(hilos.submit(vaciar));
            resultados.add(hilos.submit(vaciar));
            largada.countDown();
            for (var resultado : resultados) {
                publicados += resultado.get();
            }
        }

        assertThat((long) publicados).isEqualTo(pendientes);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox WHERE publicado_en IS NULL", Long.class)).isZero();
    }

    private List<String> tipos(String id) {
        return jdbc.queryForList("SELECT tipo_evento FROM outbox WHERE agregado_id = ? ORDER BY id",
            String.class, UUID.fromString(id));
    }

    private JsonNode payload(String id, String tipo) throws Exception {
        var texto = jdbc.queryForObject("SELECT payload::text FROM outbox WHERE agregado_id = ? AND tipo_evento = ?",
            String.class, UUID.fromString(id), tipo);
        return mapper.readTree(texto);
    }
}
