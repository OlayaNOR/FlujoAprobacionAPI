package com.nicolas.flujos.application.port.out;

import com.nicolas.flujos.application.port.in.SolicitudDetalle;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.util.Optional;

public interface IdempotenciaRepository {

    /** Devuelve false si la clave ya estaba registrada para ese usuario. */
    boolean reservar(String clave, UsuarioId usuarioId, String huella);

    Optional<RegistroIdempotencia> buscar(String clave, UsuarioId usuarioId);

    void guardarRespuesta(String clave, UsuarioId usuarioId, SolicitudDetalle respuesta);
}
