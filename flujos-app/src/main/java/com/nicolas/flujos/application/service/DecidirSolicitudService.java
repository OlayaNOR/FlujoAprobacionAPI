package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.in.DecidirSolicitudCommand;
import com.nicolas.flujos.application.port.in.DecidirSolicitudUseCase;
import com.nicolas.flujos.application.port.in.SolicitudDetalle;
import com.nicolas.flujos.application.port.out.AprobacionRepository;
import com.nicolas.flujos.application.port.out.AuditoriaRepository;
import com.nicolas.flujos.application.port.out.MetricasSolicitudes;
import com.nicolas.flujos.application.port.out.OutboxRepository;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.auditoria.AccionAuditoria;
import com.nicolas.flujos.domain.auditoria.RegistroAuditoria;
import com.nicolas.flujos.domain.flujo.AsignadorAprobador;
import com.nicolas.flujos.domain.solicitud.Decision;
import com.nicolas.flujos.domain.solicitud.ResultadoDecision;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DecidirSolicitudService implements DecidirSolicitudUseCase {

    private final AccesoSolicitudes acceso;
    private final SolicitudRepository solicitudes;
    private final AprobacionRepository aprobaciones;
    private final AuditoriaRepository auditoria;
    private final OutboxRepository outbox;
    private final MetricasSolicitudes metricas;
    private final AsignadorAprobador asignador;
    private final Clock clock;

    DecidirSolicitudService(AccesoSolicitudes acceso, SolicitudRepository solicitudes,
                            AprobacionRepository aprobaciones, AuditoriaRepository auditoria,
                            OutboxRepository outbox, MetricasSolicitudes metricas, AsignadorAprobador asignador,
                            Clock clock) {
        this.acceso = acceso;
        this.solicitudes = solicitudes;
        this.aprobaciones = aprobaciones;
        this.auditoria = auditoria;
        this.outbox = outbox;
        this.metricas = metricas;
        this.asignador = asignador;
        this.clock = clock;
    }

    // El rol se valida en el controlador. Que sea el aprobador de este paso depende del estado,
    // así que lo valida el agregado.
    @Override
    @Transactional
    public SolicitudDetalle ejecutar(DecidirSolicitudCommand c) {
        var solicitud = acceso.cargarVisible(c.solicitudId(), c.actor());
        var aprobador = c.actor().id();
        var estadoAnterior = solicitud.getEstado();
        var paso = solicitud.getPasoActual();
        var inicioPaso = solicitud.inicioPasoActual();
        var ahora = clock.instant();

        var resultado = switch (c.tipo()) {
            case APROBAR -> {
                solicitud.aprobar(aprobador, asignador, ahora);
                yield ResultadoDecision.APROBADA;
            }
            case RECHAZAR -> {
                solicitud.rechazar(aprobador, ahora);
                yield ResultadoDecision.RECHAZADA;
            }
        };

        var decision = new Decision(solicitud.getId(), paso, aprobador, resultado, c.comentario(), ahora);
        solicitudes.guardar(solicitud);
        aprobaciones.registrar(decision);
        auditoria.registrar(RegistroAuditoria.de(solicitud, accionDe(resultado), aprobador, estadoAnterior,
            detalle(paso, c.comentario()), ahora));
        solicitud.pullEventos().forEach(outbox::encolar);
        inicioPaso.ifPresent(inicio ->
            metricas.decisionTomada(solicitud.getTipo(), resultado, Duration.between(inicio, ahora)));
        return SolicitudDetalle.de(solicitud);
    }

    private static AccionAuditoria accionDe(ResultadoDecision resultado) {
        return switch (resultado) {
            case APROBADA -> AccionAuditoria.APROBADA;
            case RECHAZADA -> AccionAuditoria.RECHAZADA;
        };
    }

    private static Map<String, Object> detalle(int paso, String comentario) {
        var detalle = new HashMap<String, Object>();
        detalle.put("paso", paso);
        if (comentario != null) {
            detalle.put("comentario", comentario);
        }
        return detalle;
    }
}
