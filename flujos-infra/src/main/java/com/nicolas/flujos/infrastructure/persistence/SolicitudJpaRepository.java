package com.nicolas.flujos.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

interface SolicitudJpaRepository extends JpaRepository<SolicitudEntity, UUID>, JpaSpecificationExecutor<SolicitudEntity> {

    // usa el índice parcial idx_solicitud_estado_limite
    @Query(value = """
        SELECT id FROM solicitud
        WHERE estado = 'PENDIENTE' AND fecha_limite_paso < :ahora
        ORDER BY fecha_limite_paso
        LIMIT :lote
        """, nativeQuery = true)
    List<UUID> idsVencidas(Instant ahora, int lote);

    @Query(value = """
        SELECT * FROM solicitud
        WHERE id = :id AND estado = 'PENDIENTE' AND fecha_limite_paso < :ahora
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<SolicitudEntity> tomarVencida(UUID id, Instant ahora);
}
