package com.nicolas.flujos.application.service;

import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.shared.Periodo;
import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;

final class CamposPorTipo {

    private CamposPorTipo() {
    }

    // El dominio exige lo que falta. Esto rechaza lo que sobra, que de otro modo se perdería sin avisar.
    static void rechazarSobrantes(TipoSolicitud tipo, Money monto, Periodo periodo) {
        if (tipo == TipoSolicitud.VACACIONES && monto != null) {
            throw new ReglaNegocioException("Una solicitud de vacaciones no lleva monto");
        }
        if (tipo != TipoSolicitud.VACACIONES && periodo != null) {
            throw new ReglaNegocioException("Una solicitud de tipo " + tipo + " no lleva periodo");
        }
    }
}
