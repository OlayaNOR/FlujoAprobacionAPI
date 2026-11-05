package com.nicolas.flujos.api.error;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import com.nicolas.flujos.domain.solicitud.AccionNoPermitidaException;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.TransicionInvalidaException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ManejadorErroresTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
        .setControllerAdvice(new ManejadorErrores())
        .build();

    @Test
    void reglaDeNegocioDevuelve422() throws Exception {
        mvc.perform(get("/regla"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentType(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://flujos.api/errores/regla-negocio"))
            .andExpect(jsonPath("$.detail").value("El monto debe ser mayor que cero"));
    }

    @Test
    void transicionInvalidaDevuelve409ConEstadoActual() throws Exception {
        mvc.perform(get("/transicion"))
            .andExpect(status().isConflict())
            .andExpect(content().contentType(PROBLEM_JSON))
            .andExpect(jsonPath("$.estadoActual").value("CANCELADA"))
            .andExpect(jsonPath("$.instance").value("/transicion"));
    }

    @Test
    void accionNoPermitidaDevuelve403() throws Exception {
        mvc.perform(get("/accion")).andExpect(status().isForbidden());
    }

    @Test
    void conflictoDeConcurrenciaDevuelve409() throws Exception {
        mvc.perform(get("/concurrencia"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("https://flujos.api/errores/concurrencia"));
    }

    @Test
    void errorNoControladoNoExponeDetallesTecnicos() throws Exception {
        mvc.perform(get("/roto"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(PROBLEM_JSON))
            .andExpect(content().string(not(containsString("jdbc:postgresql"))))
            .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void validacionDevuelve400ConLosCamposQueFallaron() throws Exception {
        mvc.perform(post("/validado").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(PROBLEM_JSON))
            .andExpect(jsonPath("$.errores[0].campo").value("nombre"))
            .andExpect(jsonPath("$.errores[0].mensaje").exists());
    }

    @Test
    void jsonMalFormadoDevuelve400SinDetallesDelParser() throws Exception {
        mvc.perform(post("/validado").contentType(MediaType.APPLICATION_JSON).content("{nombre"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://flujos.api/errores/cuerpo-invalido"))
            .andExpect(content().string(not(containsString("Jackson"))));
    }

    @RestController
    static class ControladorDePrueba {

        @GetMapping("/regla")
        void regla() {
            throw new ReglaNegocioException("El monto debe ser mayor que cero");
        }

        @GetMapping("/transicion")
        void transicion() {
            throw new TransicionInvalidaException(EstadoSolicitud.CANCELADA, EstadoSolicitud.PENDIENTE);
        }

        @GetMapping("/accion")
        void accion() {
            throw new AccionNoPermitidaException("No");
        }

        @GetMapping("/concurrencia")
        void concurrencia() {
            throw new OptimisticLockingFailureException("version desactualizada");
        }

        @GetMapping("/roto")
        void roto() {
            throw new IllegalStateException("fallo en jdbc:postgresql://db:5432/flujos");
        }

        @PostMapping("/validado")
        void validado(@Valid @RequestBody Cuerpo cuerpo) {
        }

        record Cuerpo(@NotBlank String nombre) {
        }
    }
}
