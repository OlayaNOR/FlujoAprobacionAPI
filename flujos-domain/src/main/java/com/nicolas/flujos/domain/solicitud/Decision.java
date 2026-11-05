package com.nicolas.flujos.domain.solicitud;

import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.time.Instant;
import java.util.Objects;

public record Decision(SolicitudId solicitudId, int paso, UsuarioId aprobadorId, ResultadoDecision resultado,
                       String comentario, Instant decididaEn) {

    private static final int LARGO_MAXIMO_COMENTARIO = 500;

    public Decision {
        Objects.requireNonNull(solicitudId);
        Objects.requireNonNull(aprobadorId);
        Objects.requireNonNull(resultado);
        Objects.requireNonNull(decididaEn);
        if (resultado == ResultadoDecision.RECHAZADA && (comentario == null || comentario.isBlank())) {
            throw new ReglaNegocioException("Rechazar una solicitud exige indicar el motivo en el comentario");
        }
        if (comentario != null && comentario.length() > LARGO_MAXIMO_COMENTARIO) {
            throw new ReglaNegocioException(
                "El comentario no puede superar %d caracteres".formatted(LARGO_MAXIMO_COMENTARIO));
        }
    }
}
