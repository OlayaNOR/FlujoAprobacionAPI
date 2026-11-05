package com.nicolas.flujos.infrastructure.metrics;

import com.nicolas.flujos.application.port.out.MetricasSolicitudes;
import com.nicolas.flujos.domain.solicitud.ResultadoDecision;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
class MetricasMicrometer implements MetricasSolicitudes {

    private final MeterRegistry registro;

    MetricasMicrometer(MeterRegistry registro) {
        this.registro = registro;
    }

    @Override
    public void solicitudCreada(TipoSolicitud tipo) {
        alConfirmar(() -> registro.counter("solicitudes.creadas", "tipo", tipo.name()).increment());
    }

    @Override
    public void decisionTomada(TipoSolicitud tipo, ResultadoDecision resultado, Duration espera) {
        alConfirmar(() -> Timer.builder("aprobaciones.tiempo_respuesta")
            .description("Tiempo entre que un paso le llega al aprobador y su decisión")
            .tag("tipo", tipo.name())
            .tag("resultado", resultado.name())
            .register(registro)
            .record(espera));
    }

    private static void alConfirmar(Runnable registrar) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            registrar.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                registrar.run();
            }
        });
    }
}
