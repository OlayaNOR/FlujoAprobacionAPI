package com.nicolas.flujos.api.dto;

import com.nicolas.flujos.domain.flujo.RolAprobador;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PasoFlujoRequest(@Schema(example = "GERENTE") @NotNull RolAprobador rolAprobador,
                               @Schema(description = "El paso solo aplica desde este monto, o desde estos días en vacaciones. "
                                   + "Sin umbral aplica siempre", example = "1000.00")
                               @Positive @Digits(integer = 12, fraction = 2) BigDecimal umbralMinimo,
                               @Schema(example = "48") @NotNull @Min(1) @Max(8760) Integer tiempoLimiteHoras) {
}
