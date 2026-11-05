package com.nicolas.flujos.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

// El máximo es lo que cabe en NUMERIC(14,2): más que eso sería un 500 al guardar
public record MontoRequest(@Schema(example = "1500.00") @NotNull @DecimalMin("0.01") @DecimalMax("999999999999.99") BigDecimal valor,
                           @Schema(example = "USD") @NotNull @Pattern(regexp = "[A-Z]{3}") String moneda) {
}
