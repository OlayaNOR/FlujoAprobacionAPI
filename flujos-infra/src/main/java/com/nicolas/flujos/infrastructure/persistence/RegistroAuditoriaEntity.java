package com.nicolas.flujos.infrastructure.persistence;

import com.nicolas.flujos.domain.auditoria.AccionAuditoria;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Immutable
@Table(name = "registro_auditoria")
class RegistroAuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    UUID solicitudId;

    @Enumerated(EnumType.STRING)
    AccionAuditoria accion;

    UUID actorId;

    @Enumerated(EnumType.STRING)
    EstadoSolicitud estadoAnterior;

    @Enumerated(EnumType.STRING)
    EstadoSolicitud estadoNuevo;

    @JdbcTypeCode(SqlTypes.JSON)
    Map<String, Object> detalle;

    Instant ocurridoEn;
}
