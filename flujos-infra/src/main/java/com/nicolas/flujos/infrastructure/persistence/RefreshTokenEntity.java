package com.nicolas.flujos.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "refresh_token")
class RefreshTokenEntity {

    @Id
    UUID id;

    @Column(name = "usuario_id")
    UUID usuarioId;

    @Column(name = "token_hash", length = 64)
    @JdbcTypeCode(SqlTypes.CHAR)
    String tokenHash;

    @Column(name = "expira_en")
    Instant expiraEn;

    boolean revocado;

    protected RefreshTokenEntity() {
    }

    RefreshTokenEntity(UUID id, UUID usuarioId, String tokenHash, Instant expiraEn, boolean revocado) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
        this.revocado = revocado;
    }
}
