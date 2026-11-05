package com.nicolas.flujos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class AuditoriaIT extends PruebaApi {

    @Test
    void unBorradorTodaviaNoTieneAuditoria() throws Exception {
        var ana = crearUsuario();
        var id = idDe(crear(ana, COMPRA));

        auditoria(id, ana, "EMPLEADO")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void enviarDejaConstanciaDeQuienYDeLosEstados() throws Exception {
        var ana = crearUsuarioConJefe(crearAprobador());
        var id = idDe(crear(ana, COMPRA));
        accion(id, "envio", ana, "EMPLEADO");

        auditoria(id, ana, "EMPLEADO")
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].accion").value("ENVIADA"))
            .andExpect(jsonPath("$[0].actorId").value(ana.toString()))
            .andExpect(jsonPath("$[0].estadoAnterior").value("BORRADOR"))
            .andExpect(jsonPath("$[0].estadoNuevo").value("PENDIENTE"))
            .andExpect(jsonPath("$[0].detalle.paso").value(1))
            .andExpect(jsonPath("$[0].ocurridoEn").exists());
    }

    @Test
    void laAprobacionAutomaticaLaFirmaElSistema() throws Exception {
        var ana = crearUsuario();
        var id = idDe(crear(ana, "{\"tipo\":\"GASTO\",\"monto\":{\"valor\":50,\"moneda\":\"USD\"}}"));
        accion(id, "envio", ana, "EMPLEADO");

        auditoria(id, ana, "EMPLEADO")
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].accion").value("APROBADA"))
            .andExpect(jsonPath("$[0].actorId").isEmpty())
            .andExpect(jsonPath("$[0].estadoNuevo").value("APROBADA"))
            .andExpect(jsonPath("$[0].detalle.automatica").value(true))
            .andExpect(jsonPath("$[0].detalle.enviadaPor").value(ana.toString()));
    }

    @Test
    void cancelarGuardaElPasoEnQueSeCancelo() throws Exception {
        var ana = crearUsuarioConJefe(crearAprobador());
        var id = idDe(crear(ana, COMPRA));
        accion(id, "envio", ana, "EMPLEADO");
        accion(id, "cancelacion", ana, "EMPLEADO");

        auditoria(id, ana, "EMPLEADO")
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[1].accion").value("CANCELADA"))
            .andExpect(jsonPath("$[1].estadoAnterior").value("PENDIENTE"))
            .andExpect(jsonPath("$[1].detalle.paso").value(1));
    }

    @Test
    void unaTransicionRechazadaNoDejaRegistro() throws Exception {
        var ana = crearUsuario();
        var id = idDe(crear(ana, COMPRA));

        accion(id, "envio", ana, "EMPLEADO").andExpect(status().isUnprocessableEntity());

        auditoria(id, ana, "EMPLEADO").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void enviosSimultaneosDejanUnSoloRegistro() throws Exception {
        var ana = crearUsuarioConJefe(crearAprobador());
        var id = idDe(crear(ana, COMPRA));
        var largada = new CountDownLatch(1);

        try (var hilos = Executors.newFixedThreadPool(5)) {
            var resultados = new ArrayList<Future<Integer>>();
            for (int i = 0; i < 5; i++) {
                resultados.add(hilos.submit(() -> {
                    largada.await();
                    return accion(id, "envio", ana, "EMPLEADO").andReturn().getResponse().getStatus();
                }));
            }
            largada.countDown();
            for (var resultado : resultados) {
                resultado.get();
            }
        }

        var registros = jdbc.queryForObject("SELECT count(*) FROM registro_auditoria WHERE solicitud_id = ?",
            Long.class, UUID.fromString(id));
        assertThat(registros).isEqualTo(1);
    }

    @Test
    void elAdminLaPuedeConsultarYUnTerceroNo() throws Exception {
        var id = idDe(crear(crearUsuario(), COMPRA));

        auditoria(id, UUID.randomUUID(), "ADMIN").andExpect(status().isOk());
        auditoria(id, crearUsuario(), "EMPLEADO").andExpect(status().isNotFound());
    }
}
