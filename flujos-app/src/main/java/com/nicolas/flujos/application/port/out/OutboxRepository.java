package com.nicolas.flujos.application.port.out;

import com.nicolas.flujos.domain.solicitud.EventoDominio;

/** Debe llamarse dentro de la transacción del caso de uso: el evento existe solo si el cambio se confirma. */
public interface OutboxRepository {

    void encolar(EventoDominio evento);
}
