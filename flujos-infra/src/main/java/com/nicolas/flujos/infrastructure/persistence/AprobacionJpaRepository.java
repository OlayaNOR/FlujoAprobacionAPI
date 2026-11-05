package com.nicolas.flujos.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AprobacionJpaRepository extends JpaRepository<AprobacionEntity, UUID> {

    boolean existsBySolicitudIdAndAprobadorId(UUID solicitudId, UUID aprobadorId);
}
