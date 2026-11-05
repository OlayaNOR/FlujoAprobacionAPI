package com.nicolas.flujos.api.dto;

import com.nicolas.flujos.application.port.in.TipoDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecisionRequest(@Schema(example = "APROBAR") @NotNull TipoDecision tipo,
                              @Schema(description = "Obligatorio al rechazar", example = "Visto bueno")
                              @Size(max = 500) String comentario) {
}
