package com.nicolas.flujos.application.port.out;

import com.nicolas.flujos.domain.flujo.Flujo;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.util.Optional;

public interface FlujoRepository {

    Optional<Flujo> buscarPorTipo(TipoSolicitud tipo);

    /** Bloquea el flujo hasta el fin de la transacción, para que dos cambios simultáneos no se mezclen. */
    Optional<Flujo> buscarParaActualizar(TipoSolicitud tipo);

    void guardar(Flujo flujo);
}
