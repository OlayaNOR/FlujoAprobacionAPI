package com.nicolas.flujos.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface PasoAprobacionJpaRepository extends JpaRepository<PasoAprobacionEntity, UUID> {

    List<PasoAprobacionEntity> findByFlujoIdOrderByOrden(UUID flujoId);

    @Modifying
    @Query("DELETE FROM PasoAprobacionEntity p WHERE p.flujoId = :flujoId")
    void borrarDelFlujo(UUID flujoId);
}
