package com.nicolas.flujos.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record EditarSolicitudRequest(@Valid MontoRequest monto,
                                     @Valid PeriodoRequest periodo,
                                     @Size(max = 500) String motivo) {
}
