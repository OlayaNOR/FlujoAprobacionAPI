package com.nicolas.flujos.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface SolicitudJpaRepository extends JpaRepository<SolicitudEntity, UUID>, JpaSpecificationExecutor<SolicitudEntity> {
}
