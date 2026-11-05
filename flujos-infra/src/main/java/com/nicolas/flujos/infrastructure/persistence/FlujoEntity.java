package com.nicolas.flujos.infrastructure.persistence;

import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "flujo")
class FlujoEntity {

    @Id
    UUID id;

    @Enumerated(EnumType.STRING)
    TipoSolicitud tipo;

    boolean activo;

    // Cuenta los cambios de configuración. No es @Version: los cambios se serializan con un bloqueo de fila.
    long version;
}
