package com.nicolas.flujos.api.dto;

import com.nicolas.flujos.domain.flujo.RolAprobador;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PasoFlujoRequest(@NotNull RolAprobador rolAprobador,
                               @Positive @Digits(integer = 12, fraction = 2) BigDecimal umbralMinimo,
                               @NotNull @Min(1) @Max(8760) Integer tiempoLimiteHoras) {
}
