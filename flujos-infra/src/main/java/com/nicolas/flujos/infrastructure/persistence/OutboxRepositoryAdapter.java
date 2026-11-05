package com.nicolas.flujos.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicolas.flujos.application.port.out.OutboxRepository;
import com.nicolas.flujos.domain.solicitud.EventoDominio;
import com.nicolas.flujos.domain.solicitud.EventoDominio.SolicitudAprobada;
import com.nicolas.flujos.domain.solicitud.EventoDominio.SolicitudEnviada;
import com.nicolas.flujos.domain.solicitud.EventoDominio.SolicitudEscalada;
import com.nicolas.flujos.domain.solicitud.EventoDominio.SolicitudRechazada;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
class OutboxRepositoryAdapter implements OutboxRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    OutboxRepositoryAdapter(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void encolar(EventoDominio evento) {
        jdbc.update("INSERT INTO outbox (agregado_id, tipo_evento, payload) VALUES (?, ?, ?::jsonb)",
            evento.solicitudId().valor(), evento.getClass().getSimpleName(), escribir(payload(evento)));
    }

    // El payload se arma a mano y no serializando el record: es el contrato con quien consuma los eventos,
    // y no debería cambiar porque se renombre un campo del dominio.
    private static Map<String, Object> payload(EventoDominio evento) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("solicitudId", evento.solicitudId().valor().toString());
        switch (evento) {
            case SolicitudEnviada e -> datos.put("aprobadorId", e.aprobadorId().valor().toString());
            case SolicitudEscalada e -> {
                datos.put("paso", e.paso());
                datos.put("notificarAdmin", e.notificarAdmin());
            }
            case SolicitudAprobada e -> { }
            case SolicitudRechazada e -> { }
        }
        datos.put("ocurridoEn", evento.ocurridoEn().toString());
        return datos;
    }

    private String escribir(Map<String, Object> payload) {
        try {
            return json.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento", e);
        }
    }
}
