package com.nicolas.flujos.application.port.in;

import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.util.List;

public interface ReemplazarPasosFlujoUseCase {

    FlujoDetalle ejecutar(TipoSolicitud tipo, List<PasoAprobacion> pasos);
}
