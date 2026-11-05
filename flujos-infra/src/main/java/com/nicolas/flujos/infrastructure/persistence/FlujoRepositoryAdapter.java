package com.nicolas.flujos.infrastructure.persistence;

import com.nicolas.flujos.application.port.out.FlujoRepository;
import com.nicolas.flujos.domain.flujo.Flujo;
import com.nicolas.flujos.domain.flujo.FlujoId;
import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
class FlujoRepositoryAdapter implements FlujoRepository {

    private final FlujoJpaRepository flujos;
    private final PasoAprobacionJpaRepository pasos;

    FlujoRepositoryAdapter(FlujoJpaRepository flujos, PasoAprobacionJpaRepository pasos) {
        this.flujos = flujos;
        this.pasos = pasos;
    }

    @Override
    public Optional<Flujo> buscarPorTipo(TipoSolicitud tipo) {
        return flujos.findByTipo(tipo).map(this::aDominio);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Flujo> buscarParaActualizar(TipoSolicitud tipo) {
        return flujos.findByTipoConBloqueo(tipo).map(this::aDominio);
    }

    // Se borra antes de insertar porque (flujo_id, orden) es único, y al confirmar Hibernate haría
    // los INSERT antes que los DELETE. El DELETE de esta consulta se ejecuta en el momento.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void guardar(Flujo flujo) {
        var id = flujo.getId().valor();
        var entidad = flujos.findById(id).orElseGet(FlujoEntity::new);
        entidad.id = id;
        entidad.tipo = flujo.getTipo();
        entidad.activo = flujo.isActivo();
        entidad.version = flujo.getVersion();
        flujos.save(entidad);

        pasos.borrarDelFlujo(id);
        pasos.saveAll(flujo.getPasos().stream().map(paso -> aEntidad(paso, id)).toList());
        pasos.flush();
    }

    private Flujo aDominio(FlujoEntity e) {
        var deFlujo = pasos.findByFlujoIdOrderByOrden(e.id).stream()
            .map(p -> new PasoAprobacion(p.orden, p.rolAprobador, p.umbralMinimo, p.tiempoLimiteHoras))
            .toList();
        return Flujo.reconstituir(new FlujoId(e.id), e.tipo, e.activo, deFlujo, e.version);
    }

    private static PasoAprobacionEntity aEntidad(PasoAprobacion paso, UUID flujoId) {
        var entidad = new PasoAprobacionEntity();
        entidad.id = UUID.randomUUID();
        entidad.flujoId = flujoId;
        entidad.orden = paso.orden();
        entidad.rolAprobador = paso.rolAprobador();
        entidad.umbralMinimo = paso.umbralMinimo();
        entidad.tiempoLimiteHoras = paso.tiempoLimiteHoras();
        return entidad;
    }
}
