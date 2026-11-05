package com.nicolas.flujos.api.mapper;

import com.nicolas.flujos.api.dto.FlujoResponse;
import com.nicolas.flujos.api.dto.PasoFlujoResponse;
import com.nicolas.flujos.api.dto.ReemplazarPasosRequest;
import com.nicolas.flujos.application.port.in.FlujoDetalle;
import com.nicolas.flujos.domain.flujo.FlujoId;
import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FlujoMapper {

    FlujoResponse aResponse(FlujoDetalle detalle);

    PasoFlujoResponse aResponse(PasoAprobacion paso);

    default UUID uuid(FlujoId id) {
        return id.valor();
    }

    default List<PasoAprobacion> aPasos(ReemplazarPasosRequest request) {
        var pasos = new ArrayList<PasoAprobacion>();
        for (var paso : request.pasos()) {
            pasos.add(new PasoAprobacion(pasos.size() + 1, paso.rolAprobador(), paso.umbralMinimo(),
                paso.tiempoLimiteHoras()));
        }
        return pasos;
    }
}
