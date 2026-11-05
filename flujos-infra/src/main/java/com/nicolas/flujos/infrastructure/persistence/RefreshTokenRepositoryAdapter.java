package com.nicolas.flujos.infrastructure.persistence;

import com.nicolas.flujos.application.port.out.RefreshToken;
import com.nicolas.flujos.application.port.out.RefreshTokenRepository;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(RefreshToken t) {
        jpa.save(new RefreshTokenEntity(t.id(), t.usuarioId().valor(), t.tokenHash(), t.expiraEn(), t.revocado()));
    }

    @Override
    public Optional<RefreshToken> buscarConBloqueo(String tokenHash) {
        return jpa.findByTokenHash(tokenHash)
            .map(t -> new RefreshToken(t.id, new UsuarioId(t.usuarioId), t.tokenHash, t.expiraEn, t.revocado));
    }

    @Override
    public void revocar(UUID id) {
        jpa.findById(id).ifPresent(t -> t.revocado = true);
    }

    @Override
    public void revocarTodos(UsuarioId usuarioId) {
        jpa.revocarTodos(usuarioId.valor());
    }
}
