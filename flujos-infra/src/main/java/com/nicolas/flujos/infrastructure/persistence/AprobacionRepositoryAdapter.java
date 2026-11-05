package com.nicolas.flujos.infrastructure.persistence;

import com.nicolas.flujos.application.port.out.AprobacionRepository;
import com.nicolas.flujos.domain.solicitud.Decision;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class AprobacionRepositoryAdapter implements AprobacionRepository {

    private final AprobacionJpaRepository jpa;

    AprobacionRepositoryAdapter(AprobacionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void registrar(Decision d) {
        var entidad = new AprobacionEntity();
        entidad.id = UUID.randomUUID();
        entidad.solicitudId = d.solicitudId().valor();
        entidad.paso = d.paso();
        entidad.aprobadorId = d.aprobadorId().valor();
        entidad.decision = d.resultado();
        entidad.comentario = d.comentario();
        entidad.decididaEn = d.decididaEn();
        jpa.save(entidad);
    }

    @Override
    public boolean participo(SolicitudId solicitudId, UsuarioId usuarioId) {
        return jpa.existsBySolicitudIdAndAprobadorId(solicitudId.valor(), usuarioId.valor());
    }
}
