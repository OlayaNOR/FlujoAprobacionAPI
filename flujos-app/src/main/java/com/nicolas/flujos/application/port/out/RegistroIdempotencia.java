package com.nicolas.flujos.application.port.out;

import com.nicolas.flujos.application.port.in.SolicitudDetalle;

public record RegistroIdempotencia(String huella, SolicitudDetalle respuesta) {
}
