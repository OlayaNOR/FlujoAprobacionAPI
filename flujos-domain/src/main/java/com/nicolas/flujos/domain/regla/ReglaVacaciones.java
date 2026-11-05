package com.nicolas.flujos.domain.regla;

import static com.nicolas.flujos.domain.flujo.RolAprobador.JEFE;
import static com.nicolas.flujos.domain.flujo.RolAprobador.RRHH;

import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.time.Duration;
import java.util.List;

public final class ReglaVacaciones implements ReglaAprobacion {

    private static final int MAXIMO_DIAS_SOLO_JEFE = 3;

    private final Duration tiempoLimite;

    public ReglaVacaciones(Duration tiempoLimite) {
        this.tiempoLimite = tiempoLimite;
    }

    @Override
    public TipoSolicitud tipo() {
        return TipoSolicitud.VACACIONES;
    }

    @Override
    public List<PasoAprobacion> pasosRequeridos(Solicitud solicitud) {
        if (solicitud.getPeriodo().dias() <= MAXIMO_DIAS_SOLO_JEFE) {
            return PasoAprobacion.secuencia(tiempoLimite, JEFE);
        }
        return PasoAprobacion.secuencia(tiempoLimite, JEFE, RRHH);
    }

    @Override
    public Duration tiempoLimitePorPaso() {
        return tiempoLimite;
    }
}
