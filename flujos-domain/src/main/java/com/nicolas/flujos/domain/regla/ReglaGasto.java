package com.nicolas.flujos.domain.regla;

import static com.nicolas.flujos.domain.flujo.RolAprobador.JEFE;

import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.time.Duration;
import java.util.List;

public final class ReglaGasto implements ReglaAprobacion {

    private static final Money APROBACION_AUTOMATICA_BAJO = Money.de("200", "USD");

    private final Duration tiempoLimite;

    public ReglaGasto(Duration tiempoLimite) {
        this.tiempoLimite = tiempoLimite;
    }

    @Override
    public TipoSolicitud tipo() {
        return TipoSolicitud.GASTO;
    }

    @Override
    public List<PasoAprobacion> pasosRequeridos(Solicitud solicitud) {
        if (solicitud.getMonto().esMenorQue(APROBACION_AUTOMATICA_BAJO)) {
            return List.of();
        }
        return PasoAprobacion.secuencia(tiempoLimite, JEFE);
    }

    @Override
    public Duration tiempoLimitePorPaso() {
        return tiempoLimite;
    }
}
