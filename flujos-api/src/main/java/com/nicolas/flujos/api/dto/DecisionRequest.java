package com.nicolas.flujos.api.dto;

import com.nicolas.flujos.application.port.in.TipoDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecisionRequest(@NotNull TipoDecision tipo, @Size(max = 500) String comentario) {
}
