package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.in.CrearSolicitudCommand;
import com.nicolas.flujos.application.port.in.CrearSolicitudUseCase;
import com.nicolas.flujos.application.port.in.SolicitudDetalle;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Currency;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CrearSolicitudService implements CrearSolicitudUseCase {

    private final SolicitudRepository solicitudes;
    private final Clock clock;
    private final Currency monedaBase;

    CrearSolicitudService(SolicitudRepository solicitudes, Clock clock,
                          @Value("${app.negocio.moneda-base}") Currency monedaBase) {
        this.solicitudes = solicitudes;
        this.clock = clock;
        this.monedaBase = monedaBase;
    }

    @Override
    @Transactional
    public SolicitudDetalle ejecutar(CrearSolicitudCommand c) {
        CamposPorTipo.rechazarSobrantes(c.tipo(), c.monto(), c.periodo());
        var ahora = clock.instant();
        var solicitud = switch (c.tipo()) {
            case COMPRA -> Solicitud.nuevaCompra(c.solicitanteId(), c.monto(), c.motivo(), monedaBase, ahora);
            case GASTO -> Solicitud.nuevoGasto(c.solicitanteId(), c.monto(), c.motivo(), monedaBase, ahora);
            case VACACIONES -> Solicitud.nuevasVacaciones(c.solicitanteId(), c.periodo(), c.motivo(),
                LocalDate.now(clock), ahora);
        };
        solicitudes.guardar(solicitud);
        return SolicitudDetalle.de(solicitud);
    }
}
