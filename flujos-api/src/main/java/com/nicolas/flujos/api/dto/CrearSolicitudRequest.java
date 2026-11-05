package com.nicolas.flujos.api.dto;

import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearSolicitudRequest(@NotNull TipoSolicitud tipo,
                                    @Valid MontoRequest monto,
                                    @Valid PeriodoRequest periodo,
                                    @Size(max = 500) String motivo) {
}
