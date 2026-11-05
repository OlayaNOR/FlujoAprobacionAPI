package com.nicolas.flujos.domain.regla;

import static com.nicolas.flujos.domain.flujo.RolAprobador.DIRECTOR;
import static com.nicolas.flujos.domain.flujo.RolAprobador.GERENTE;
import static com.nicolas.flujos.domain.flujo.RolAprobador.JEFE;

import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.time.Duration;
import java.util.List;

public final class ReglaCompra implements ReglaAprobacion {

    private static final Money DESDE_GERENTE = Money.de("1000", "USD");
    private static final Money HASTA_GERENTE = Money.de("10000", "USD");

    private final Duration tiempoLimite;

    public ReglaCompra(Duration tiempoLimite) {
        this.tiempoLimite = tiempoLimite;
    }

    @Override
    public TipoSolicitud tipo() {
        return TipoSolicitud.COMPRA;
    }

    @Override
    public List<PasoAprobacion> pasosRequeridos(Solicitud solicitud) {
        var monto = solicitud.getMonto();
        if (monto.esMenorQue(DESDE_GERENTE)) {
            return PasoAprobacion.secuencia(tiempoLimite, JEFE);
        }
        if (monto.esMayorQue(HASTA_GERENTE)) {
            return PasoAprobacion.secuencia(tiempoLimite, JEFE, GERENTE, DIRECTOR);
        }
        return PasoAprobacion.secuencia(tiempoLimite, JEFE, GERENTE);
    }

    @Override
    public Duration tiempoLimitePorPaso() {
        return tiempoLimite;
    }
}
