package com.nicolas.flujos.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class PublicadorEnLog implements PublicadorEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorEnLog.class);

    @Override
    public void publicar(EventoPendiente evento) {
        log.info("Evento {} de la solicitud {}: {}", evento.tipo(), evento.agregadoId(), evento.payload());
    }
}
