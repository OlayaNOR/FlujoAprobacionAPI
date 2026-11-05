package com.nicolas.flujos.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicolas.flujos.application.port.in.SolicitudDetalle;
import com.nicolas.flujos.application.port.out.IdempotenciaRepository;
import com.nicolas.flujos.application.port.out.RegistroIdempotencia;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class IdempotenciaRepositoryAdapter implements IdempotenciaRepository {

    private static final int CREADA = 201;

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    IdempotenciaRepositoryAdapter(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    // Con la misma clave sin confirmar en otra transacción, este INSERT espera a que termine. Si se confirmó no
    // inserta nada, y si se revirtió la reserva pasa a ser de esta petición. El status 0 nunca se llega a ver:
    // guardarRespuesta lo reemplaza antes de confirmar.
    @Override
    public boolean reservar(String clave, UsuarioId usuarioId, String huella) {
        return jdbc.update("""
            INSERT INTO idempotencia (clave, usuario_id, hash_request, status_code)
            VALUES (?, ?, ?, 0)
            ON CONFLICT (clave, usuario_id) DO NOTHING
            """, clave, usuarioId.valor(), huella) == 1;
    }

    @Override
    public Optional<RegistroIdempotencia> buscar(String clave, UsuarioId usuarioId) {
        return jdbc.query("SELECT hash_request, respuesta FROM idempotencia WHERE clave = ? AND usuario_id = ?",
                (fila, n) -> new RegistroIdempotencia(fila.getString("hash_request"), leer(fila.getString("respuesta"))),
                clave, usuarioId.valor())
            .stream().findFirst();
    }

    @Override
    public void guardarRespuesta(String clave, UsuarioId usuarioId, SolicitudDetalle respuesta) {
        jdbc.update("UPDATE idempotencia SET respuesta = ?::jsonb, status_code = ? WHERE clave = ? AND usuario_id = ?",
            escribir(respuesta), CREADA, clave, usuarioId.valor());
    }

    private String escribir(SolicitudDetalle respuesta) {
        try {
            return json.writeValueAsString(respuesta);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la respuesta idempotente", e);
        }
    }

    private SolicitudDetalle leer(String respuesta) {
        try {
            return json.readValue(respuesta, SolicitudDetalle.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("La respuesta idempotente guardada no se puede leer", e);
        }
    }
}
