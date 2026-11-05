package com.nicolas.flujos.infrastructure.persistence;

import com.nicolas.flujos.domain.flujo.RolAprobador;
import com.nicolas.flujos.domain.usuario.Rol;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, UUID> {

    Optional<UsuarioEntity> findByEmail(String email);

    @Query("""
        select u.id from UsuarioEntity u join u.roles r
        where u.cargo = :cargo and u.activo = true and u.id <> :excluido and r = :rol
        order by u.creadoEn, u.id
        """)
    List<UUID> buscarIdsConCargo(@Param("cargo") RolAprobador cargo, @Param("excluido") UUID excluido,
                                 @Param("rol") Rol rol, Limit limite);
}
