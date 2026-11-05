package com.nicolas.flujos.infrastructure.persistence;

import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "solicitud")
class SolicitudEntity {

    @Id
    UUID id;

    UUID solicitanteId;

    @Enumerated(EnumType.STRING)
    TipoSolicitud tipo;

    @Enumerated(EnumType.STRING)
    EstadoSolicitud estado;

    BigDecimal monto;

    @Column(length = 3)
    @JdbcTypeCode(SqlTypes.CHAR)
    String moneda;

    LocalDate periodoInicio;

    LocalDate periodoFin;

    String motivo;

    @JdbcTypeCode(SqlTypes.JSON)
    List<PasoAprobacion> pasos;

    int pasoActual;

    UUID aprobadorActualId;

    Instant fechaLimitePaso;

    // Long y no long: en null Spring Data sabe que es nueva y hace persist en vez de merge
    @Version
    Long version;

    Instant creadaEn;

    Instant actualizadaEn;
}
