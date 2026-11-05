package com.nicolas.flujos.infrastructure.scheduling;

import com.nicolas.flujos.application.port.in.EscalarVencidasUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class JobEscalamiento {

    private static final Logger log = LoggerFactory.getLogger(JobEscalamiento.class);

    private final EscalarVencidasUseCase escalar;

    JobEscalamiento(EscalarVencidasUseCase escalar) {
        this.escalar = escalar;
    }

    @Scheduled(cron = "${app.jobs.escalamiento.cron:0 */10 * * * *}")
    void ejecutar() {
        var resultado = escalar.ejecutar();
        if (resultado.escaladas() > 0) {
            log.info("Escalamiento: {} solicitudes escaladas", resultado.escaladas());
        }
        resultado.fallidas().forEach(fallo ->
            log.warn("No se pudo escalar la solicitud {}: {}", fallo.solicitudId().valor(), fallo.motivo()));
    }
}
