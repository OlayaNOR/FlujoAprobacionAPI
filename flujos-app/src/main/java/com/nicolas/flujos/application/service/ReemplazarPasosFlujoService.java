package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.in.FlujoDetalle;
import com.nicolas.flujos.application.port.in.ReemplazarPasosFlujoUseCase;
import com.nicolas.flujos.application.port.out.FlujoRepository;
import com.nicolas.flujos.domain.flujo.FlujoNoEncontradoException;
import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReemplazarPasosFlujoService implements ReemplazarPasosFlujoUseCase {

    private final FlujoRepository flujos;

    ReemplazarPasosFlujoService(FlujoRepository flujos) {
        this.flujos = flujos;
    }

    @Override
    @Transactional
    public FlujoDetalle ejecutar(TipoSolicitud tipo, List<PasoAprobacion> pasos) {
        var flujo = flujos.buscarParaActualizar(tipo).orElseThrow(FlujoNoEncontradoException::new);
        flujo.reemplazarPasos(pasos);
        flujos.guardar(flujo);
        return FlujoDetalle.de(flujo);
    }
}
