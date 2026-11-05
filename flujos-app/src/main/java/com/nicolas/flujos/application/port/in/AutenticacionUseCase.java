package com.nicolas.flujos.application.port.in;

import com.nicolas.flujos.domain.usuario.UsuarioId;

public interface AutenticacionUseCase {

    TokensSesion iniciarSesion(String email, String password);

    TokensSesion renovar(String refreshToken);

    void cerrarSesion(UsuarioId actor, String refreshToken);
}
