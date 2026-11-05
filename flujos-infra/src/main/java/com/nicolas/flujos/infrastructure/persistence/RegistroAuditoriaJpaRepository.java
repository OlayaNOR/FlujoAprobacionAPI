package com.nicolas.flujos.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RegistroAuditoriaJpaRepository extends JpaRepository<RegistroAuditoriaEntity, Long> {

    List<RegistroAuditoriaEntity> findBySolicitudIdOrderByOcurridoEnAscIdAsc(UUID solicitudId);
}
