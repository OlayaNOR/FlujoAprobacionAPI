package com.nicolas.flujos.application.port.out;

import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    void guardar(RefreshToken token);

    /** Bloquea la fila hasta el fin de la transacción: dos renovaciones simultáneas no pueden usar el mismo token. */
    Optional<RefreshToken> buscarConBloqueo(String tokenHash);

    void revocar(UUID id);

    void revocarTodos(UsuarioId usuarioId);
}
