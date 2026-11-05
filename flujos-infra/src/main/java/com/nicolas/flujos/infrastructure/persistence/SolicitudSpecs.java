package com.nicolas.flujos.infrastructure.persistence;

import com.nicolas.flujos.application.port.in.FiltroSolicitud;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

final class SolicitudSpecs {

    private SolicitudSpecs() {
    }

    static Specification<SolicitudEntity> filtrar(FiltroSolicitud filtro, UUID visiblePara) {
        return (root, query, cb) -> {
            var predicados = new ArrayList<Predicate>();

            if (visiblePara != null) {
                predicados.add(cb.or(
                    cb.equal(root.get("solicitanteId"), visiblePara),
                    cb.equal(root.get("aprobadorActualId"), visiblePara)));
            }
            if (!filtro.estados().isEmpty()) {
                predicados.add(root.get("estado").in(filtro.estados()));
            }
            if (filtro.tipo() != null) {
                predicados.add(cb.equal(root.get("tipo"), filtro.tipo()));
            }
            if (filtro.desde() != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.<Instant>get("creadaEn"), filtro.desde()));
            }
            if (filtro.hasta() != null) {
                predicados.add(cb.lessThanOrEqualTo(root.<Instant>get("creadaEn"), filtro.hasta()));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
