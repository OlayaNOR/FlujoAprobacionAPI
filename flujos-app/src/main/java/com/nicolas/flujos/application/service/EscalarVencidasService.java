package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.in.EscalarVencidasUseCase;
import com.nicolas.flujos.application.port.in.ResultadoEscalamiento;
import com.nicolas.flujos.application.port.in.ResultadoEscalamiento.Fallo;
import com.nicolas.flujos.application.port.out.AuditoriaRepository;
import com.nicolas.flujos.application.port.out.OutboxRepository;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.auditoria.AccionAuditoria;
import com.nicolas.flujos.domain.auditoria.RegistroAuditoria;
import com.nicolas.flujos.domain.flujo.AsignadorAprobador;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
class EscalarVencidasService implements EscalarVencidasUseCase {

    // Lo que no entre en una corrida queda para la siguiente
    private static final int LOTE = 100;

    private final SolicitudRepository solicitudes;
    private final AuditoriaRepository auditoria;
    private final OutboxRepository outbox;
    private final AsignadorAprobador asignador;
    private final TransactionTemplate transaccion;
    private final Clock clock;

    EscalarVencidasService(SolicitudRepository solicitudes, AuditoriaRepository auditoria, OutboxRepository outbox,
                           AsignadorAprobador asignador, PlatformTransactionManager transacciones, Clock clock) {
        this.solicitudes = solicitudes;
        this.auditoria = auditoria;
        this.outbox = outbox;
        this.asignador = asignador;
        this.transaccion = new TransactionTemplate(transacciones);
        this.clock = clock;
    }

    // Una transacción por solicitud: si una falla (no hay a quién asignar el paso, o alguien la decidió
    // mientras tanto), las demás se escalan igual y esa se reintenta en la siguiente corrida.
    @Override
    public ResultadoEscalamiento ejecutar() {
        var ahora = clock.instant();
        var escaladas = 0;
        var fallidas = new ArrayList<Fallo>();
        for (var id : solicitudes.vencidas(ahora, LOTE)) {
            try {
                if (Boolean.TRUE.equals(transaccion.execute(estado -> escalar(id, ahora)))) {
                    escaladas++;
                }
            } catch (RuntimeException e) {
                fallidas.add(new Fallo(id, Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName())));
            }
        }
        return new ResultadoEscalamiento(escaladas, List.copyOf(fallidas));
    }

    private boolean escalar(SolicitudId id, Instant ahora) {
        var tomada = solicitudes.tomarVencida(id, ahora);
        if (tomada.isEmpty()) {
            return false;
        }
        var solicitud = tomada.get();
        var pasoAnterior = solicitud.getPasoActual();

        solicitud.escalar(asignador, ahora);

        solicitudes.guardar(solicitud);
        var alAdmin = solicitud.getPasoActual() == pasoAnterior;
        auditoria.registrar(alAdmin
            ? RegistroAuditoria.de(solicitud, AccionAuditoria.ESCALADA_ADMIN, null, EstadoSolicitud.PENDIENTE,
                Map.of("paso", pasoAnterior), ahora)
            : RegistroAuditoria.de(solicitud, AccionAuditoria.ESCALADA, null, EstadoSolicitud.PENDIENTE,
                Map.of("pasoAnterior", pasoAnterior, "paso", solicitud.getPasoActual()), ahora));
        solicitud.pullEventos().forEach(outbox::encolar);
        return true;
    }
}
