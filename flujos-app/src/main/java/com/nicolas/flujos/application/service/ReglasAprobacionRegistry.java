package com.nicolas.flujos.application.service;

import com.nicolas.flujos.domain.regla.ReglaAprobacion;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class ReglasAprobacionRegistry {

    private final Map<TipoSolicitud, ReglaAprobacion> reglas;

    ReglasAprobacionRegistry(List<ReglaAprobacion> todas) {
        this.reglas = todas.stream()
            .collect(Collectors.toUnmodifiableMap(ReglaAprobacion::tipo, Function.identity()));
    }

    ReglaAprobacion para(TipoSolicitud tipo) {
        return Optional.ofNullable(reglas.get(tipo))
            .orElseThrow(() -> new IllegalStateException("Sin regla para " + tipo));
    }
}
