package com.nicolas.flujos.api.controller;

import com.nicolas.flujos.api.dto.CrearSolicitudRequest;
import com.nicolas.flujos.api.dto.EditarSolicitudRequest;
import com.nicolas.flujos.api.dto.SolicitudResponse;
import com.nicolas.flujos.api.mapper.SolicitudMapper;
import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.in.ConsultarSolicitudUseCase;
import com.nicolas.flujos.application.port.in.CrearSolicitudUseCase;
import com.nicolas.flujos.application.port.in.EditarSolicitudUseCase;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(SolicitudController.RUTA)
class SolicitudController {

    static final String RUTA = "/api/v1/solicitudes";

    private final CrearSolicitudUseCase crear;
    private final ConsultarSolicitudUseCase consultar;
    private final EditarSolicitudUseCase editar;
    private final SolicitudMapper mapper;

    SolicitudController(CrearSolicitudUseCase crear, ConsultarSolicitudUseCase consultar,
                        EditarSolicitudUseCase editar, SolicitudMapper mapper) {
        this.crear = crear;
        this.consultar = consultar;
        this.editar = editar;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLEADO')")
    ResponseEntity<SolicitudResponse> crear(@AuthenticationPrincipal Actor actor,
                                            @Valid @RequestBody CrearSolicitudRequest request) {
        var creada = mapper.aResponse(crear.ejecutar(mapper.aComando(request, actor)));
        return ResponseEntity.created(URI.create(RUTA + "/" + creada.id())).body(creada);
    }

    @GetMapping("/{id}")
    SolicitudResponse consultar(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return mapper.aResponse(consultar.ejecutar(new SolicitudId(id), actor));
    }

    @PutMapping("/{id}")
    SolicitudResponse editar(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                             @Valid @RequestBody EditarSolicitudRequest request) {
        return mapper.aResponse(editar.ejecutar(mapper.aComando(request, new SolicitudId(id), actor)));
    }
}
