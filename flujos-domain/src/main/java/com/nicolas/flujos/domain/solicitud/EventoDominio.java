package com.nicolas.flujos.domain.solicitud;

import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.time.Instant;

public sealed interface EventoDominio {

    SolicitudId solicitudId();

    Instant ocurridoEn();

    record SolicitudEnviada(SolicitudId solicitudId, UsuarioId aprobadorId, Instant ocurridoEn)
        implements EventoDominio {}

    record SolicitudAprobada(SolicitudId solicitudId, Instant ocurridoEn)
        implements EventoDominio {}

    record SolicitudRechazada(SolicitudId solicitudId, Instant ocurridoEn)
        implements EventoDominio {}

    /** Con {@code notificarAdmin} en true ya no quedaban pasos y la solicitud sigue en el último. */
    record SolicitudEscalada(SolicitudId solicitudId, int paso, boolean notificarAdmin, Instant ocurridoEn)
        implements EventoDominio {}
}
