package com.nicolas.flujos.api.controller;

import com.nicolas.flujos.api.doc.ErroresPosibles;
import com.nicolas.flujos.api.dto.FlujoResponse;
import com.nicolas.flujos.api.dto.ReemplazarPasosRequest;
import com.nicolas.flujos.api.mapper.FlujoMapper;
import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.in.ConsultarFlujoUseCase;
import com.nicolas.flujos.application.port.in.ReemplazarPasosFlujoUseCase;
import com.nicolas.flujos.domain.flujo.FlujoNoEncontradoException;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flujos")
@Tag(name = "Flujos", description = "Pasos de aprobación de cada tipo de solicitud")
class FlujoController {

    private static final Logger log = LoggerFactory.getLogger(FlujoController.class);

    private final ConsultarFlujoUseCase consultar;
    private final ReemplazarPasosFlujoUseCase reemplazar;
    private final FlujoMapper mapper;

    FlujoController(ConsultarFlujoUseCase consultar, ReemplazarPasosFlujoUseCase reemplazar, FlujoMapper mapper) {
        this.consultar = consultar;
        this.reemplazar = reemplazar;
        this.mapper = mapper;
    }

    @GetMapping("/{tipo}")
    @Operation(summary = "Consulta los pasos de un tipo de solicitud")
    @ErroresPosibles(404)
    FlujoResponse consultar(@Parameter(schema = @Schema(implementation = TipoSolicitud.class)) @PathVariable String tipo) {
        return mapper.aResponse(consultar.ejecutar(tipoDe(tipo)));
    }

    @PutMapping("/{tipo}")
    @Operation(summary = "Reemplaza los pasos de un flujo",
        description = "El orden de los pasos es su posición en la lista. Las solicitudes que ya están "
            + "en aprobación siguen con los pasos que tenían al enviarse.")
    @ErroresPosibles({400, 403, 404, 422})
    @PreAuthorize("hasRole('ADMIN')")
    FlujoResponse reemplazar(@AuthenticationPrincipal Actor actor,
                             @Parameter(schema = @Schema(implementation = TipoSolicitud.class)) @PathVariable String tipo,
                             @Valid @RequestBody ReemplazarPasosRequest request) {
        var flujo = reemplazar.ejecutar(tipoDe(tipo), mapper.aPasos(request));
        // Cambia quién aprueba qué, y la auditoría solo cubre solicitudes: esta línea es el único rastro.
        log.info("Flujo {} reemplazado por {}: versión {}, pasos {}", flujo.tipo(), actor.id().valor(),
            flujo.version(), flujo.pasos());
        return mapper.aResponse(flujo);
    }

    // Se recibe como texto para que un tipo que no existe sea 404, igual que un flujo que no existe.
    private static TipoSolicitud tipoDe(String valor) {
        try {
            return TipoSolicitud.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new FlujoNoEncontradoException();
        }
    }
}
