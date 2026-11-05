package com.nicolas.flujos.infrastructure.persistence;

import com.nicolas.flujos.domain.flujo.RolAprobador;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "paso_aprobacion")
class PasoAprobacionEntity {

    @Id
    UUID id;

    UUID flujoId;

    int orden;

    @Enumerated(EnumType.STRING)
    RolAprobador rolAprobador;

    BigDecimal umbralMinimo;

    int tiempoLimiteHoras;
}
