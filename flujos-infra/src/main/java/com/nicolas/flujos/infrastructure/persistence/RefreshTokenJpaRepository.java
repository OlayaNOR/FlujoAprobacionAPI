package com.nicolas.flujos.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query("update RefreshTokenEntity t set t.revocado = true where t.usuarioId = :usuarioId and t.revocado = false")
    void revocarTodos(@Param("usuarioId") UUID usuarioId);
}
