package com.nicolas.flujos.application.port.in;

import com.nicolas.flujos.domain.flujo.Flujo;
import com.nicolas.flujos.domain.flujo.FlujoId;
import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.util.List;

public record FlujoDetalle(FlujoId id, TipoSolicitud tipo, boolean activo, long version, List<PasoAprobacion> pasos) {

    public static FlujoDetalle de(Flujo f) {
        return new FlujoDetalle(f.getId(), f.getTipo(), f.isActivo(), f.getVersion(), f.getPasos());
    }
}
