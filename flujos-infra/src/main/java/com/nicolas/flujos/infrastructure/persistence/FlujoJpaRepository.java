package com.nicolas.flujos.infrastructure.persistence;

import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface FlujoJpaRepository extends JpaRepository<FlujoEntity, UUID> {

    Optional<FlujoEntity> findByTipo(TipoSolicitud tipo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FlujoEntity f WHERE f.tipo = :tipo")
    Optional<FlujoEntity> findByTipoConBloqueo(TipoSolicitud tipo);
}
