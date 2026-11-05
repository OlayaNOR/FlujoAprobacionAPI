package com.nicolas.flujos.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** El orden de los pasos es su posición en la lista. */
public record ReemplazarPasosRequest(@NotNull List<@NotNull @Valid PasoFlujoRequest> pasos) {
}
