package com.nicolas.flujos.api.dto;

import com.nicolas.flujos.domain.flujo.RolAprobador;
import java.math.BigDecimal;

public record PasoFlujoResponse(int orden, RolAprobador rolAprobador, BigDecimal umbralMinimo, int tiempoLimiteHoras) {
}
