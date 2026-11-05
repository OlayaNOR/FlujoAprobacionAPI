package com.nicolas.flujos.api.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PeriodoRequest(@NotNull @FutureOrPresent LocalDate inicio, @NotNull LocalDate fin) {
}
