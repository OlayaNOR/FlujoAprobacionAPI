package com.nicolas.flujos.application.port.in;

public record PaginaSolicitada(int numero, int tamano, CampoOrden campo, boolean ascendente) {
}
