package com.nicolas.flujos.application.port.out;

import com.nicolas.flujos.domain.usuario.Usuario;

public record CuentaUsuario(Usuario usuario, String passwordHash) {
}
