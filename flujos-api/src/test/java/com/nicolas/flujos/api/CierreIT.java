package com.nicolas.flujos.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CierreIT extends PruebaApi {

    private static final String GASTO_MENOR = "{\"tipo\":\"GASTO\",\"monto\":{\"valor\":50,\"moneda\":\"USD\"}}";

    @Test
    void unAdminCierraUnaSolicitudAprobadaYQuedaEnLaAuditoria() throws Exception {
        var ana = crearUsuario();
        var admin = crearUsuario();
        var id = aprobada(ana);

        accion(id, "cierre", admin, "ADMIN")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CERRADA"));

        auditoria(id, ana, "EMPLEADO")
            .andExpect(jsonPath("$[-1:].accion").value("CERRADA"))
            .andExpect(jsonPath("$[-1:].actorId").value(admin.toString()))
            .andExpect(jsonPath("$[-1:].estadoAnterior").value("APROBADA"));
    }

    @Test
    void cerrarDosVecesDevuelve409() throws Exception {
        var admin = crearUsuario();
        var id = aprobada(crearUsuario());
        accion(id, "cierre", admin, "ADMIN");

        accion(id, "cierre", admin, "ADMIN")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.estadoActual").value("CERRADA"));
    }

    @Test
    void unaSolicitudSinAprobarNoSePuedeCerrar() throws Exception {
        var id = idDe(crear(crearUsuario(), GASTO_MENOR));

        accion(id, "cierre", crearUsuario(), "ADMIN")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.estadoActual").value("BORRADOR"));
    }

    @Test
    void elSolicitanteNoPuedeCerrarSuSolicitud() throws Exception {
        var ana = crearUsuario();
        var id = aprobada(ana);

        accion(id, "cierre", ana, "EMPLEADO").andExpect(status().isForbidden());
        consultar(id, ana, "EMPLEADO").andExpect(jsonPath("$.estado").value("APROBADA"));
    }

    @Test
    void unaSolicitudInexistenteDevuelve404() throws Exception {
        accion(UUID.randomUUID().toString(), "cierre", crearUsuario(), "ADMIN").andExpect(status().isNotFound());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(post(RUTA + "/" + UUID.randomUUID() + "/cierre").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    private String aprobada(UUID solicitante) throws Exception {
        var id = idDe(crear(solicitante, GASTO_MENOR));
        accion(id, "envio", solicitante, "EMPLEADO").andExpect(jsonPath("$.estado").value("APROBADA"));
        return id;
    }
}
