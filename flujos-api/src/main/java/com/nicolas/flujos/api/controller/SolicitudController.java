package com.nicolas.flujos.api.controller;

import com.nicolas.flujos.api.dto.CrearSolicitudRequest;
import com.nicolas.flujos.api.dto.DecisionRequest;
import com.nicolas.flujos.api.dto.EditarSolicitudRequest;
import com.nicolas.flujos.api.dto.RegistroAuditoriaResponse;
import com.nicolas.flujos.api.dto.SolicitudResponse;
import com.nicolas.flujos.api.dto.SolicitudResumenResponse;
import com.nicolas.flujos.api.mapper.SolicitudMapper;
import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.in.CancelarSolicitudUseCase;
import com.nicolas.flujos.application.port.in.CerrarSolicitudUseCase;
import com.nicolas.flujos.application.port.in.ConsultarAuditoriaUseCase;
import com.nicolas.flujos.application.port.in.ConsultarSolicitudUseCase;
import com.nicolas.flujos.application.port.in.CrearSolicitudUseCase;
import com.nicolas.flujos.application.port.in.DecidirSolicitudCommand;
import com.nicolas.flujos.application.port.in.DecidirSolicitudUseCase;
import com.nicolas.flujos.application.port.in.EditarSolicitudUseCase;
import com.nicolas.flujos.application.port.in.EnviarSolicitudUseCase;
import com.nicolas.flujos.application.port.in.FiltroSolicitud;
import com.nicolas.flujos.application.port.in.ListarSolicitudesUseCase;
import com.nicolas.flujos.application.port.in.Pagina;
import com.nicolas.flujos.application.port.in.PaginaSolicitada;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(SolicitudController.RUTA)
class SolicitudController {

    static final String RUTA = "/api/v1/solicitudes";

    private final CrearSolicitudUseCase crear;
    private final ConsultarSolicitudUseCase consultar;
    private final EditarSolicitudUseCase editar;
    private final ListarSolicitudesUseCase listar;
    private final EnviarSolicitudUseCase enviar;
    private final CancelarSolicitudUseCase cancelar;
    private final DecidirSolicitudUseCase decidir;
    private final CerrarSolicitudUseCase cerrar;
    private final ConsultarAuditoriaUseCase consultarAuditoria;
    private final SolicitudMapper mapper;

    SolicitudController(CrearSolicitudUseCase crear, ConsultarSolicitudUseCase consultar,
                        EditarSolicitudUseCase editar, ListarSolicitudesUseCase listar,
                        EnviarSolicitudUseCase enviar, CancelarSolicitudUseCase cancelar,
                        DecidirSolicitudUseCase decidir, CerrarSolicitudUseCase cerrar,
                        ConsultarAuditoriaUseCase consultarAuditoria, SolicitudMapper mapper) {
        this.crear = crear;
        this.consultar = consultar;
        this.editar = editar;
        this.listar = listar;
        this.enviar = enviar;
        this.cancelar = cancelar;
        this.decidir = decidir;
        this.cerrar = cerrar;
        this.consultarAuditoria = consultarAuditoria;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLEADO')")
    ResponseEntity<SolicitudResponse> crear(@AuthenticationPrincipal Actor actor,
                                            @Valid @RequestBody CrearSolicitudRequest request) {
        var creada = mapper.aResponse(crear.ejecutar(mapper.aComando(request, actor)));
        return ResponseEntity.created(URI.create(RUTA + "/" + creada.id())).body(creada);
    }

    @GetMapping
    Pagina<SolicitudResumenResponse> listar(@AuthenticationPrincipal Actor actor,
                                            @RequestParam(required = false) Set<EstadoSolicitud> estado,
                                            @RequestParam(required = false) TipoSolicitud tipo,
                                            @RequestParam(required = false) Instant desde,
                                            @RequestParam(required = false) Instant hasta,
                                            @RequestParam(defaultValue = "0") @Min(0) int page,
                                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                            @RequestParam(defaultValue = "creadaEn,desc") String sort) {
        var orden = OrdenListado.de(sort);
        return listar.ejecutar(new FiltroSolicitud(estado, tipo, desde, hasta),
                new PaginaSolicitada(page, size, orden.campo(), orden.ascendente()), actor)
            .map(mapper::aResponse);
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

    @GetMapping("/{id}/auditoria")
    List<RegistroAuditoriaResponse> auditoria(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return consultarAuditoria.ejecutar(new SolicitudId(id), actor).stream().map(mapper::aResponse).toList();
    }

    @PostMapping("/{id}/envio")
    SolicitudResponse enviar(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return mapper.aResponse(enviar.ejecutar(new SolicitudId(id), actor));
    }

    @PostMapping("/{id}/decisiones")
    @PreAuthorize("hasRole('APROBADOR')")
    SolicitudResponse decidir(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                              @Valid @RequestBody DecisionRequest request) {
        return mapper.aResponse(decidir.ejecutar(
            new DecidirSolicitudCommand(new SolicitudId(id), actor, request.tipo(), request.comentario())));
    }

    @PostMapping("/{id}/cancelacion")
    SolicitudResponse cancelar(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return mapper.aResponse(cancelar.ejecutar(new SolicitudId(id), actor));
    }

    @PostMapping("/{id}/cierre")
    @PreAuthorize("hasRole('ADMIN')")
    SolicitudResponse cerrar(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return mapper.aResponse(cerrar.ejecutar(new SolicitudId(id), actor));
    }
}
