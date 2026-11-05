package com.nicolas.flujos.application.port.in;

import com.nicolas.flujos.domain.solicitud.TipoSolicitud;

public interface ConsultarFlujoUseCase {

    FlujoDetalle ejecutar(TipoSolicitud tipo);
}
