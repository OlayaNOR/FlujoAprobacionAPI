package com.nicolas.flujos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ContratoOpenApiIT extends PruebaApi {

    // Si el cambio en la API es a propósito: ./mvnw verify -Dcontrato.actualizar=true y revisar el diff del archivo
    private static final Path CONTRATO = Path.of("src/test/resources/contrato/openapi.json");

    @Test
    void elContratoGeneradoEsElGuardado() throws Exception {
        var json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        var generado = (ObjectNode) mapper.readTree(json);
        generado.remove("servers"); // sale del host que hace la petición

        if (Boolean.getBoolean("contrato.actualizar")) {
            Files.createDirectories(CONTRATO.getParent());
            Files.writeString(CONTRATO, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(generado) + "\n");
        }

        var guardado = mapper.readTree(Files.readString(CONTRATO));
        assertThat(generado.equals(guardado))
            .as("El OpenAPI cambió respecto a %s. Si es a propósito, regenerarlo con -Dcontrato.actualizar=true", CONTRATO)
            .isTrue();
    }

    @Test
    void laDocumentacionNoPideToken() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().isFound())
            .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }
}
