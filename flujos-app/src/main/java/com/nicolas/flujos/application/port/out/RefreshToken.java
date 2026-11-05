package com.nicolas.flujos.application.port.out;

import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.time.Instant;
import java.util.UUID;

public record RefreshToken(UUID id, UsuarioId usuarioId, String tokenHash, Instant expiraEn, boolean revocado) {

    public boolean expiro(Instant ahora) {
        return !expiraEn.isAfter(ahora);
    }
}
