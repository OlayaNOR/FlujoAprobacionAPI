package com.nicolas.flujos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class FlujosIT extends PruebaApi {

    private static final String RUTA_FLUJOS = "/api/v1/flujos/";
    private static final String APROBAR = "{\"tipo\":\"APROBAR\"}";

    private static final String COMPRA_INICIAL = pasos(paso("JEFE", null, 48), paso("GERENTE", "1000", 48),
        paso("DIRECTOR", "10000.01", 48));
    private static final String GASTO_INICIAL = pasos(paso("JEFE", "200", 48));
    private static final String VACACIONES_INICIAL = pasos(paso("JEFE", null, 48), paso("RRHH", "4", 48));

    // Los flujos son compartidos por todas las pruebas de la API
    @AfterEach
    void restaurarFlujos() throws Exception {
        jdbc.update("UPDATE flujo SET activo = TRUE");
        reemplazar("COMPRA", COMPRA_INICIAL).andExpect(status().isOk());
        reemplazar("GASTO", GASTO_INICIAL).andExpect(status().isOk());
        reemplazar("VACACIONES", VACACIONES_INICIAL).andExpect(status().isOk());
    }

    @Test
    void cualquierUsuarioAutenticadoVeLosPasos() throws Exception {
        consultarFlujo("COMPRA")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("COMPRA"))
            .andExpect(jsonPath("$.activo").value(true))
            .andExpect(jsonPath("$.pasos.length()").value(3))
            .andExpect(jsonPath("$.pasos[0].orden").value(1))
            .andExpect(jsonPath("$.pasos[0].rolAprobador").value("JEFE"))
            .andExpect(jsonPath("$.pasos[0].umbralMinimo").doesNotExist())
            .andExpect(jsonPath("$.pasos[0].tiempoLimiteHoras").value(48))
            .andExpect(jsonPath("$.pasos[2].umbralMinimo").value(10000.01));
    }

    @Test
    void consultarSinTokenDevuelve401() throws Exception {
        mvc.perform(get(RUTA_FLUJOS + "COMPRA")).andExpect(status().isUnauthorized());
    }

    @Test
    void unTipoQueNoExisteDevuelve404() throws Exception {
        consultarFlujo("ACCESOS")
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(PROBLEM_JSON));
        reemplazar("ACCESOS", GASTO_INICIAL).andExpect(status().isNotFound());
    }

    @Test
    void unAdminReemplazaLosPasosYSubeLaVersion() throws Exception {
        var version = versionDe("GASTO");

        reemplazar("GASTO", pasos(paso("GERENTE", "50", 24), paso("JEFE", null, 72)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(version + 1))
            .andExpect(jsonPath("$.pasos[1].orden").value(2));

        consultarFlujo("GASTO")
            .andExpect(jsonPath("$.pasos.length()").value(2))
            .andExpect(jsonPath("$.pasos[0].rolAprobador").value("GERENTE"))
            .andExpect(jsonPath("$.pasos[0].umbralMinimo").value(50.0))
            .andExpect(jsonPath("$.pasos[1].tiempoLimiteHoras").value(72));
    }

    @Test
    void loQueDevuelveElGetSePuedeEnviarAlPut() throws Exception {
        var actual = consultarFlujo("VACACIONES").andReturn().getResponse().getContentAsString();

        reemplazar("VACACIONES", actual)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pasos.length()").value(2))
            .andExpect(jsonPath("$.pasos[1].rolAprobador").value("RRHH"));
    }

    @Test
    void soloUnAdminPuedeReemplazarLosPasos() throws Exception {
        mvc.perform(put(RUTA_FLUJOS + "COMPRA")
                .header(HttpHeaders.AUTHORIZATION, bearer(crearUsuario(), "APROBADOR", "EMPLEADO"))
                .contentType(MediaType.APPLICATION_JSON).content(GASTO_INICIAL))
            .andExpect(status().isForbidden());
        mvc.perform(put(RUTA_FLUJOS + "COMPRA").contentType(MediaType.APPLICATION_JSON).content(GASTO_INICIAL))
            .andExpect(status().isUnauthorized());

        consultarFlujo("COMPRA").andExpect(jsonPath("$.pasos.length()").value(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"pasos\":[null]}",
        "{\"pasos\":[{\"tiempoLimiteHoras\":48}]}",
        "{\"pasos\":[{\"rolAprobador\":\"CEO\",\"tiempoLimiteHoras\":48}]}",
        "{\"pasos\":[{\"rolAprobador\":\"JEFE\"}]}",
        "{\"pasos\":[{\"rolAprobador\":\"JEFE\",\"tiempoLimiteHoras\":0}]}",
        "{\"pasos\":[{\"rolAprobador\":\"JEFE\",\"tiempoLimiteHoras\":8761}]}",
        "{\"pasos\":[{\"rolAprobador\":\"JEFE\",\"tiempoLimiteHoras\":48,\"umbralMinimo\":0}]}",
        "{\"pasos\":[{\"rolAprobador\":\"JEFE\",\"tiempoLimiteHoras\":48,\"umbralMinimo\":10.001}]}",
        "{\"pasos\":[{\"rolAprobador\":\"JEFE\",\"tiempoLimiteHoras\":48,\"umbralMinimo\":1000000000000}]}"
    })
    void unCuerpoMalFormadoDevuelve400(String cuerpo) throws Exception {
        reemplazar("COMPRA", cuerpo)
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(PROBLEM_JSON));

        consultarFlujo("COMPRA").andExpect(jsonPath("$.pasos.length()").value(3));
    }

    @Test
    void lasReglasDelFlujoDevuelven422YNoCambianNada() throws Exception {
        var version = versionDe("VACACIONES");

        reemplazar("VACACIONES", "{\"pasos\":[]}").andExpect(status().isUnprocessableEntity());
        reemplazar("VACACIONES", pasos(paso("JEFE", null, 48), paso("JEFE", "4", 48)))
            .andExpect(status().isUnprocessableEntity());
        reemplazar("VACACIONES", pasos(paso("JEFE", "3.5", 48))).andExpect(status().isUnprocessableEntity());

        consultarFlujo("VACACIONES")
            .andExpect(jsonPath("$.pasos.length()").value(2))
            .andExpect(jsonPath("$.version").value(version));
    }

    @Test
    void losFlujosInicialesRespetanLosLimitesDeLasVacaciones() throws Exception {
        crearConCargo("RRHH");
        var jefe = crearAprobador();
        var ana = crearUsuarioConJefe(jefe);

        var cortas = enviada(ana, vacacionesDe(3));
        var largas = enviada(ana, vacacionesDe(4));

        decidir(cortas, jefe, APROBAR).andExpect(jsonPath("$.estado").value("APROBADA"));
        decidir(largas, jefe, APROBAR)
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.pasoActual").value(2));
    }

    @Test
    void losFlujosInicialesApruebanSoloLosGastosMenoresA200() throws Exception {
        var ana = crearUsuarioConJefe(crearAprobador());

        accion(idDe(crear(ana, gastoDe("199.99"))), "envio", ana, "EMPLEADO")
            .andExpect(jsonPath("$.estado").value("APROBADA"));
        accion(idDe(crear(ana, gastoDe("200"))), "envio", ana, "EMPLEADO")
            .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void unaSolicitudNuevaUsaLosPasosConfigurados() throws Exception {
        crearConCargo("DIRECTOR");
        reemplazar("COMPRA", pasos(paso("DIRECTOR", null, 48))).andExpect(status().isOk());
        var ana = crearUsuario();

        var enviadaSinJefe = accion(idDe(crear(ana, compraDe("500"))), "envio", ana, "EMPLEADO")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pasoActual").value(1));

        var aprobador = siguienteAprobador(enviadaSinJefe);
        assertThat(cargoDe(aprobador)).isEqualTo("DIRECTOR");
        var id = idDe(enviadaSinJefe);
        decidir(id, aprobador, APROBAR).andExpect(jsonPath("$.estado").value("APROBADA"));
    }

    @Test
    void subirUnUmbralCambiaLoQueSeApruebaSolo() throws Exception {
        reemplazar("GASTO", pasos(paso("JEFE", "1000", 48))).andExpect(status().isOk());
        var ana = crearUsuario();

        accion(idDe(crear(ana, gastoDe("999.99"))), "envio", ana, "EMPLEADO")
            .andExpect(jsonPath("$.estado").value("APROBADA"));
    }

    @Test
    void elTiempoLimiteSaleDelPasoConfigurado() throws Exception {
        reemplazar("COMPRA", pasos(paso("JEFE", null, 1))).andExpect(status().isOk());
        var ana = crearUsuarioConJefe(crearAprobador());
        var antes = Instant.now();

        var respuesta = accion(idDe(crear(ana, compraDe("500"))), "envio", ana, "EMPLEADO")
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        var limite = Instant.parse(mapper.readTree(respuesta).get("fechaLimitePaso").asText());
        assertThat(limite).isBetween(antes.plus(Duration.ofHours(1)), Instant.now().plus(Duration.ofHours(1)));
    }

    @Test
    void cambiarElFlujoNoAfectaALasSolicitudesEnCurso() throws Exception {
        crearConCargo("GERENTE");
        crearConCargo("DIRECTOR");
        reemplazar("COMPRA", pasos(paso("JEFE", null, 48), paso("GERENTE", null, 48))).andExpect(status().isOk());
        var jefe = crearAprobador();
        var ana = crearUsuarioConJefe(jefe);
        var enCurso = enviada(ana, compraDe("500"));

        reemplazar("COMPRA", pasos(paso("DIRECTOR", null, 48))).andExpect(status().isOk());

        var gerente = siguienteAprobador(decidir(enCurso, jefe, APROBAR)
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.pasoActual").value(2)));
        assertThat(cargoDe(gerente)).isEqualTo("GERENTE");
        decidir(enCurso, gerente, APROBAR).andExpect(jsonPath("$.estado").value("APROBADA"));

        var nueva = enviada(ana, compraDe("500"));
        var director = siguienteAprobador(consultar(nueva, ana, "EMPLEADO"));
        assertThat(cargoDe(director)).isEqualTo("DIRECTOR");
    }

    @Test
    void unBorradorUsaElFlujoVigenteAlEnviarseNoAlCrearse() throws Exception {
        crearConCargo("DIRECTOR");
        var ana = crearUsuarioConJefe(crearAprobador());
        var id = idDe(crear(ana, compraDe("500")));

        reemplazar("COMPRA", pasos(paso("DIRECTOR", null, 48))).andExpect(status().isOk());

        var aprobador = siguienteAprobador(accion(id, "envio", ana, "EMPLEADO"));
        assertThat(cargoDe(aprobador)).isEqualTo("DIRECTOR");
    }

    @Test
    void conElFlujoDesactivadoNoSePuedeEnviar() throws Exception {
        jdbc.update("UPDATE flujo SET activo = FALSE WHERE tipo = 'COMPRA'");
        var ana = crearUsuarioConJefe(crearAprobador());
        var id = idDe(crear(ana, compraDe("500")));

        accion(id, "envio", ana, "EMPLEADO").andExpect(status().isUnprocessableEntity());

        consultar(id, ana, "EMPLEADO").andExpect(jsonPath("$.estado").value("BORRADOR"));
        consultarFlujo("COMPRA").andExpect(jsonPath("$.activo").value(false));
    }

    private ResultActions consultarFlujo(String tipo) throws Exception {
        return mvc.perform(get(RUTA_FLUJOS + tipo).header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID(), "EMPLEADO")));
    }

    private ResultActions reemplazar(String tipo, String cuerpo) throws Exception {
        return mvc.perform(put(RUTA_FLUJOS + tipo)
            .header(HttpHeaders.AUTHORIZATION, bearer(crearUsuario(), "ADMIN"))
            .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private long versionDe(String tipo) throws Exception {
        var cuerpo = consultarFlujo(tipo).andReturn().getResponse().getContentAsString();
        return mapper.readTree(cuerpo).get("version").asLong();
    }

    private String cargoDe(UUID usuario) {
        return jdbc.queryForObject("SELECT cargo FROM usuario WHERE id = ?", String.class, usuario);
    }

    private static String pasos(String... pasos) {
        return "{\"pasos\":[" + String.join(",", pasos) + "]}";
    }

    private static String paso(String rol, String umbral, int horas) {
        var conUmbral = umbral == null ? "" : ",\"umbralMinimo\":" + umbral;
        return "{\"rolAprobador\":\"%s\",\"tiempoLimiteHoras\":%d%s}".formatted(rol, horas, conUmbral);
    }

    private static String compraDe(String monto) {
        return "{\"tipo\":\"COMPRA\",\"monto\":{\"valor\":%s,\"moneda\":\"USD\"}}".formatted(monto);
    }

    private static String gastoDe(String monto) {
        return "{\"tipo\":\"GASTO\",\"monto\":{\"valor\":%s,\"moneda\":\"USD\"}}".formatted(monto);
    }

    private static String vacacionesDe(int dias) {
        var inicio = LocalDate.now().plusDays(10);
        return "{\"tipo\":\"VACACIONES\",\"periodo\":{\"inicio\":\"%s\",\"fin\":\"%s\"}}"
            .formatted(inicio, inicio.plusDays(dias - 1L));
    }
}
