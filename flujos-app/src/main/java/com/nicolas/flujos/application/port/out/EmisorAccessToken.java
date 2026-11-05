package com.nicolas.flujos.application.port.out;

import com.nicolas.flujos.domain.usuario.Usuario;
import java.time.Duration;

public interface EmisorAccessToken {

    String emitir(Usuario usuario);

    Duration vigencia();
}
