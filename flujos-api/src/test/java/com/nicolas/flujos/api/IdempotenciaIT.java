package com.nicolas.flujos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class IdempotenciaIT extends PruebaApi {

    private static final String CLAVE = "7c1f0e3a-2b8d-4f7e-9a61-0d5c2e8b9f10";

    @Test
    void repetirConLaMismaClaveDevuelveLaMismaRespuestaSinDuplicar() throws Exception {
        var ana = crearUsuario();

        var primera = crearConClave(ana, CLAVE, COMPRA).andExpect(status().isCreated());
        var id = idDe(primera);

        crearConClave(ana, CLAVE, COMPRA)
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.LOCATION, RUTA + "/" + id))
            .andExpect(content().json(primera.andReturn().getResponse().getContentAsString(), true));

        assertThat(solicitudesDe(ana)).isEqualTo(1);
    }

    @Test
    void elOrdenDeLasLlavesYLaEscalaDelMontoNoCambianLaPeticion() throws Exception {
        var ana = crearUsuario();
        var id = idDe(crearConClave(ana, CLAVE, "{\"tipo\":\"COMPRA\",\"monto\":{\"valor\":100,\"moneda\":\"USD\"}}"));

        var repetida = crearConClave(ana, CLAVE, "{\"monto\":{\"moneda\":\"USD\",\"valor\":100.00},\"tipo\":\"COMPRA\"}")
            .andExpect(status().isCreated());

        assertThat(idDe(repetida)).isEqualTo(id);
    }

    @Test
    void laMismaClaveConOtroCuerpoDevuelve422() throws Exception {
        var ana = crearUsuario();
        crearConClave(ana, CLAVE, COMPRA).andExpect(status().isCreated());

        crearConClave(ana, CLAVE, "{\"tipo\":\"COMPRA\",\"monto\":{\"valor\":99,\"moneda\":\"USD\"}}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentType(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://flujos.api/errores/clave-idempotencia-reutilizada"));

        assertThat(solicitudesDe(ana)).isEqualTo(1);
    }

    @Test
    void laClaveEsDeCadaUsuario() throws Exception {
        var idDeAna = idDe(crearConClave(crearUsuario(), CLAVE, COMPRA));
        var idDeLuis = idDe(crearConClave(crearUsuario(), CLAVE, COMPRA).andExpect(status().isCreated()));

        assertThat(idDeLuis).isNotEqualTo(idDeAna);
    }

    @Test
    void unReintentoDevuelveLaRespuestaOriginalAunqueLaSolicitudHayaAvanzado() throws Exception {
        var ana = crearUsuarioConJefe(crearAprobador());
        var id = idDe(crearConClave(ana, CLAVE, COMPRA));
        accion(id, "envio", ana, "EMPLEADO").andExpect(jsonPath("$.estado").value("PENDIENTE"));

        crearConClave(ana, CLAVE, COMPRA)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    void unaCreacionFallidaNoGastaLaClave() throws Exception {
        var ana = crearUsuario();

        crearConClave(ana, CLAVE, "{\"tipo\":\"COMPRA\",\"monto\":{\"valor\":100,\"moneda\":\"EUR\"}}")
            .andExpect(status().isUnprocessableEntity());

        crearConClave(ana, CLAVE, COMPRA).andExpect(status().isCreated());
        assertThat(solicitudesDe(ana)).isEqualTo(1);
    }

    @Test
    void peticionesSimultaneasConLaMismaClaveCreanUnaSolaSolicitud() throws Exception {
        var ana = crearUsuario();
        var largada = new CountDownLatch(1);
        var ids = new HashSet<String>();

        try (var hilos = Executors.newFixedThreadPool(5)) {
            var resultados = new ArrayList<Future<ResultActions>>();
            for (int i = 0; i < 5; i++) {
                resultados.add(hilos.submit(() -> {
                    largada.await();
                    return crearConClave(ana, CLAVE, COMPRA);
                }));
            }
            largada.countDown();
            for (var resultado : resultados) {
                ids.add(idDe(resultado.get().andExpect(status().isCreated())));
            }
        }

        assertThat(ids).hasSize(1);
        assertThat(solicitudesDe(ana)).isEqualTo(1);
    }

    @Test
    void sinClaveCadaPeticionCreaUnaSolicitud() throws Exception {
        var ana = crearUsuario();

        crear(ana, COMPRA).andExpect(status().isCreated());
        crear(ana, COMPRA).andExpect(status().isCreated());

        assertThat(solicitudesDe(ana)).isEqualTo(2);
    }

    @Test
    void unaClaveVaciaODemasiadoLargaDevuelve400() throws Exception {
        var ana = crearUsuario();

        crearConClave(ana, " ", COMPRA).andExpect(status().isBadRequest());
        crearConClave(ana, "x".repeat(101), COMPRA).andExpect(status().isBadRequest());
        crearConClave(ana, "x".repeat(100), COMPRA).andExpect(status().isCreated());

        assertThat(solicitudesDe(ana)).isEqualTo(1);
    }

    private ResultActions crearConClave(UUID usuario, String clave, String cuerpo) throws Exception {
        return mvc.perform(post(RUTA)
            .header(HttpHeaders.AUTHORIZATION, bearer(usuario, "EMPLEADO"))
            .header("Idempotency-Key", clave)
            .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private Long solicitudesDe(UUID usuario) {
        return jdbc.queryForObject("SELECT count(*) FROM solicitud WHERE solicitante_id = ?", Long.class, usuario);
    }
}
