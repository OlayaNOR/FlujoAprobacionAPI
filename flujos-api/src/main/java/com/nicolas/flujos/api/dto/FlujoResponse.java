package com.nicolas.flujos.api.dto;

import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.util.List;
import java.util.UUID;

public record FlujoResponse(UUID id, TipoSolicitud tipo, boolean activo, long version, List<PasoFlujoResponse> pasos) {
}
