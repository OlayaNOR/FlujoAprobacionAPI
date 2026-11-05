package com.nicolas.flujos.api.dto;

import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearSolicitudRequest(@Schema(example = "COMPRA") @NotNull TipoSolicitud tipo,
                                    @Schema(description = "Obligatorio en COMPRA y GASTO") @Valid MontoRequest monto,
                                    @Schema(description = "Obligatorio en VACACIONES") @Valid PeriodoRequest periodo,
                                    @Schema(example = "Monitor para el puesto de diseño") @Size(max = 500) String motivo) {
}
