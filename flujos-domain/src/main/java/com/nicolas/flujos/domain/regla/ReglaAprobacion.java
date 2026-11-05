package com.nicolas.flujos.domain.regla;

import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.time.Duration;
import java.util.List;

public interface ReglaAprobacion {

    TipoSolicitud tipo();

    /** Una lista vacía significa que la solicitud se aprueba sin intervención de nadie. */
    List<PasoAprobacion> pasosRequeridos(Solicitud solicitud);

    Duration tiempoLimitePorPaso();
}
