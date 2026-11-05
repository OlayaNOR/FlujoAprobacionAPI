package com.nicolas.flujos.infrastructure.messaging;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publica los eventos del outbox. La garantía es "al menos una vez": si la aplicación cae después de publicar
 * y antes de confirmar, el lote se vuelve a publicar en la siguiente corrida.
 */
@Component
public class RelevoOutbox {

    private static final Logger log = LoggerFactory.getLogger(RelevoOutbox.class);
    private static final int LOTE = 50;

    private final JdbcTemplate jdbc;
    private final PublicadorEventos publicador;
    private final Clock clock;

    RelevoOutbox(JdbcTemplate jdbc, PublicadorEventos publicador, Clock clock) {
        this.jdbc = jdbc;
        this.publicador = publicador;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.jobs.outbox.intervalo:PT2S}")
    void programado() {
        publicarPendientes();
    }

    /**
     * Publica un lote y devuelve cuántos eventos marcó. SKIP LOCKED deja que otra instancia tome las filas
     * siguientes en vez de esperar, y ninguna publica las que otra tiene tomadas.
     */
    @Transactional
    public int publicarPendientes() {
        var pendientes = jdbc.query("""
                SELECT id, agregado_id, tipo_evento, payload, creado_en FROM outbox
                WHERE publicado_en IS NULL
                ORDER BY creado_en, id
                LIMIT ?
                FOR UPDATE SKIP LOCKED
                """,
            (fila, n) -> new EventoPendiente(fila.getLong("id"), fila.getObject("agregado_id", UUID.class),
                fila.getString("tipo_evento"), fila.getString("payload"), fila.getTimestamp("creado_en").toInstant()),
            LOTE);

        var publicados = new ArrayList<Object[]>();
        var ahora = Timestamp.from(clock.instant());
        for (var evento : pendientes) {
            try {
                publicador.publicar(evento);
            } catch (RuntimeException e) {
                // Se corta para no publicar fuera de orden. Los ya publicados se marcan igual.
                log.warn("No se pudo publicar el evento {}. Se reintenta en la siguiente corrida", evento.id(), e);
                break;
            }
            publicados.add(new Object[] {ahora, evento.id()});
        }
        jdbc.batchUpdate("UPDATE outbox SET publicado_en = ? WHERE id = ?", publicados);
        return publicados.size();
    }
}
