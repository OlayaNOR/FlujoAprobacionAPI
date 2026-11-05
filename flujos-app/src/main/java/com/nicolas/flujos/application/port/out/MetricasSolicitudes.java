package com.nicolas.flujos.application.port.out;

import com.nicolas.flujos.domain.solicitud.ResultadoDecision;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.time.Duration;

/** Se registran al confirmar la transacción: un cambio revertido no cuenta. */
public interface MetricasSolicitudes {

    void solicitudCreada(TipoSolicitud tipo);

    /** @param espera desde que el paso le llegó al aprobador hasta que decidió */
    void decisionTomada(TipoSolicitud tipo, ResultadoDecision resultado, Duration espera);
}
