package com.nicolas.flujos.infrastructure.metrics;

import com.nicolas.flujos.domain.solicitud.EstadoSolicitud;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * El gauge lee un valor en memoria que se actualiza cada minuto. Si consultara la base en cada lectura,
 * cada scrape de métricas costaría una consulta.
 */
@Component
public class SolicitudesPorEstado {

    private final JdbcTemplate jdbc;
    private final Map<EstadoSolicitud, AtomicLong> conteos = new EnumMap<>(EstadoSolicitud.class);

    SolicitudesPorEstado(JdbcTemplate jdbc, MeterRegistry registro) {
        this.jdbc = jdbc;
        for (var estado : EstadoSolicitud.values()) {
            var conteo = new AtomicLong();
            conteos.put(estado, conteo);
            Gauge.builder("solicitudes.por_estado", conteo, AtomicLong::get)
                .description("Solicitudes en cada estado, actualizado cada minuto")
                .tag("estado", estado.name())
                .register(registro);
        }
    }

    @Scheduled(fixedDelayString = "${app.jobs.metricas.intervalo:PT1M}")
    public void actualizar() {
        var nuevos = new EnumMap<EstadoSolicitud, Long>(EstadoSolicitud.class);
        jdbc.query("SELECT estado, count(*) AS total FROM solicitud GROUP BY estado",
            fila -> { nuevos.put(EstadoSolicitud.valueOf(fila.getString("estado")), fila.getLong("total")); });
        // un estado sin filas no aparece en el GROUP BY y tiene que volver a 0
        conteos.forEach((estado, conteo) -> conteo.set(nuevos.getOrDefault(estado, 0L)));
    }
}
