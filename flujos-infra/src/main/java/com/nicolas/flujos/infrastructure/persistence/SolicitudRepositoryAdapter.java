package com.nicolas.flujos.infrastructure.persistence;

import com.nicolas.flujos.application.port.in.FiltroSolicitud;
import com.nicolas.flujos.application.port.in.Pagina;
import com.nicolas.flujos.application.port.in.PaginaSolicitada;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.shared.Periodo;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
class SolicitudRepositoryAdapter implements SolicitudRepository {

    private final SolicitudJpaRepository jpa;

    SolicitudRepositoryAdapter(SolicitudJpaRepository jpa) {
        this.jpa = jpa;
    }

    // MANDATORY porque el bloqueo optimista depende de cargar y guardar en la misma transacción:
    // findById devuelve la entidad con la versión que se leyó, y el UPDATE falla si otro la cambió después.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void guardar(Solicitud s) {
        var entidad = jpa.findById(s.getId().valor()).orElseGet(SolicitudEntity::new);
        entidad.id = s.getId().valor();
        entidad.solicitanteId = s.getSolicitanteId().valor();
        entidad.tipo = s.getTipo();
        entidad.estado = s.getEstado();
        entidad.monto = s.getMonto() == null ? null : s.getMonto().valor();
        entidad.moneda = s.getMonto() == null ? null : s.getMonto().moneda().getCurrencyCode();
        entidad.periodoInicio = s.getPeriodo() == null ? null : s.getPeriodo().inicio();
        entidad.periodoFin = s.getPeriodo() == null ? null : s.getPeriodo().fin();
        entidad.motivo = s.getMotivo();
        entidad.pasoActual = s.getPasoActual();
        entidad.aprobadorActualId = s.getAprobadorActualId() == null ? null : s.getAprobadorActualId().valor();
        entidad.fechaLimitePaso = s.getFechaLimitePaso();
        entidad.creadaEn = s.getCreadaEn();
        entidad.actualizadaEn = s.getActualizadaEn();
        // Hibernate hace los INSERT antes que los UPDATE al confirmar. Con flush aquí, un conflicto de versión
        // aparece antes de escribir la decisión o la auditoría, y no como una violación de clave única.
        jpa.saveAndFlush(entidad);
    }

    @Override
    public Optional<Solicitud> buscarPorId(SolicitudId id) {
        return jpa.findById(id.valor()).map(SolicitudRepositoryAdapter::aDominio);
    }

    @Override
    public Pagina<Solicitud> listar(FiltroSolicitud filtro, UsuarioId visiblePara, PaginaSolicitada pagina) {
        var usuario = visiblePara == null ? null : visiblePara.valor();
        var resultado = jpa.findAll(SolicitudSpecs.filtrar(filtro, usuario),
            PageRequest.of(pagina.numero(), pagina.tamano(), orden(pagina)));
        return new Pagina<>(resultado.getContent().stream().map(SolicitudRepositoryAdapter::aDominio).toList(),
            resultado.getNumber(), resultado.getSize(), resultado.getTotalElements(), resultado.getTotalPages());
    }

    // El id desempata: sin él, filas con el mismo valor pueden repetirse o perderse entre páginas.
    private static Sort orden(PaginaSolicitada pagina) {
        var atributo = switch (pagina.campo()) {
            case CREADA_EN -> "creadaEn";
            case MONTO -> "monto";
            case ESTADO -> "estado";
        };
        var direccion = pagina.ascendente() ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direccion, atributo).and(Sort.by("id"));
    }

    private static Solicitud aDominio(SolicitudEntity e) {
        var monto = e.monto == null ? null : new Money(e.monto, Currency.getInstance(e.moneda));
        var periodo = e.periodoInicio == null ? null : new Periodo(e.periodoInicio, e.periodoFin);
        return Solicitud.reconstituir(new SolicitudId(e.id), new UsuarioId(e.solicitanteId), e.tipo, monto, periodo,
            e.motivo, e.estado, e.pasoActual, aId(e.aprobadorActualId), e.fechaLimitePaso, e.version,
            e.creadaEn, e.actualizadaEn);
    }

    private static UsuarioId aId(UUID id) {
        return id == null ? null : new UsuarioId(id);
    }
}
