package com.nicolas.flujos.infrastructure.persistence;

import com.nicolas.flujos.domain.solicitud.ResultadoDecision;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "aprobacion")
class AprobacionEntity {

    @Id
    UUID id;

    UUID solicitudId;

    int paso;

    UUID aprobadorId;

    @Enumerated(EnumType.STRING)
    ResultadoDecision decision;

    String comentario;

    Instant decididaEn;
}
