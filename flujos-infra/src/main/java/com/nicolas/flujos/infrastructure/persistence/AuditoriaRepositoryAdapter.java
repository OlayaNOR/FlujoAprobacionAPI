package com.nicolas.flujos.infrastructure.persistence;

import com.nicolas.flujos.application.port.out.AuditoriaRepository;
import com.nicolas.flujos.domain.auditoria.RegistroAuditoria;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class AuditoriaRepositoryAdapter implements AuditoriaRepository {

    private final RegistroAuditoriaJpaRepository jpa;

    AuditoriaRepositoryAdapter(RegistroAuditoriaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void registrar(RegistroAuditoria r) {
        var entidad = new RegistroAuditoriaEntity();
        entidad.solicitudId = r.solicitudId().valor();
        entidad.accion = r.accion();
        entidad.actorId = r.actorId() == null ? null : r.actorId().valor();
        entidad.estadoAnterior = r.estadoAnterior();
        entidad.estadoNuevo = r.estadoNuevo();
        entidad.detalle = r.detalle();
        entidad.ocurridoEn = r.ocurridoEn();
        jpa.save(entidad);
    }

    @Override
    public List<RegistroAuditoria> listarPorSolicitud(SolicitudId id) {
        return jpa.findBySolicitudIdOrderByOcurridoEnAscIdAsc(id.valor()).stream()
            .map(e -> new RegistroAuditoria(new SolicitudId(e.solicitudId), e.accion,
                e.actorId == null ? null : new UsuarioId(e.actorId), e.estadoAnterior, e.estadoNuevo,
                e.detalle, e.ocurridoEn))
            .toList();
    }
}
