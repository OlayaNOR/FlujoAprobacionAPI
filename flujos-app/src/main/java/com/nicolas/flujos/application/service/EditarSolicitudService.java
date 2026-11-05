package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.in.EditarSolicitudCommand;
import com.nicolas.flujos.application.port.in.EditarSolicitudUseCase;
import com.nicolas.flujos.application.port.in.SolicitudDetalle;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Currency;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class EditarSolicitudService implements EditarSolicitudUseCase {

    private final AccesoSolicitudes acceso;
    private final SolicitudRepository solicitudes;
    private final Clock clock;
    private final Currency monedaBase;

    EditarSolicitudService(AccesoSolicitudes acceso, SolicitudRepository solicitudes, Clock clock,
                           @Value("${app.negocio.moneda-base}") Currency monedaBase) {
        this.acceso = acceso;
        this.solicitudes = solicitudes;
        this.clock = clock;
        this.monedaBase = monedaBase;
    }

    @Override
    @Transactional
    public SolicitudDetalle ejecutar(EditarSolicitudCommand c) {
        var solicitud = acceso.cargarDelSolicitante(c.solicitudId(), c.actor());
        CamposPorTipo.rechazarSobrantes(solicitud.getTipo(), c.monto(), c.periodo());

        var ahora = clock.instant();
        if (solicitud.getTipo() == TipoSolicitud.VACACIONES) {
            solicitud.editarPeriodo(c.periodo(), c.motivo(), LocalDate.now(clock), ahora);
        } else {
            solicitud.editarMonto(c.monto(), c.motivo(), monedaBase, ahora);
        }
        solicitudes.guardar(solicitud);
        return SolicitudDetalle.de(solicitud);
    }
}
