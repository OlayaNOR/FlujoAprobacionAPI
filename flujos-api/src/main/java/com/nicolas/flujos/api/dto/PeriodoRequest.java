package com.nicolas.flujos.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PeriodoRequest(@Schema(example = "2026-12-14") @NotNull @FutureOrPresent LocalDate inicio,
                             @Schema(example = "2026-12-18") @NotNull LocalDate fin) {
}
