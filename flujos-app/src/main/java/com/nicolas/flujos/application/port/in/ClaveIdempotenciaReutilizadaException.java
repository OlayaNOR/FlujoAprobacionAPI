package com.nicolas.flujos.application.port.in;

import com.nicolas.flujos.domain.shared.ReglaNegocioException;

public class ClaveIdempotenciaReutilizadaException extends ReglaNegocioException {

    public ClaveIdempotenciaReutilizadaException() {
        super("La clave de idempotencia ya se usó con otro cuerpo. Usa una clave nueva para esta petición.");
    }
}
