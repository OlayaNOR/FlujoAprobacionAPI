package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.in.ConsultarFlujoUseCase;
import com.nicolas.flujos.application.port.in.FlujoDetalle;
import com.nicolas.flujos.application.port.out.FlujoRepository;
import com.nicolas.flujos.domain.flujo.FlujoNoEncontradoException;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ConsultarFlujoService implements ConsultarFlujoUseCase {

    private final FlujoRepository flujos;

    ConsultarFlujoService(FlujoRepository flujos) {
        this.flujos = flujos;
    }

    @Override
    @Transactional(readOnly = true)
    public FlujoDetalle ejecutar(TipoSolicitud tipo) {
        return flujos.buscarPorTipo(tipo).map(FlujoDetalle::de).orElseThrow(FlujoNoEncontradoException::new);
    }
}
