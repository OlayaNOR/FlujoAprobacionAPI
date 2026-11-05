package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.in.ClaveIdempotenciaReutilizadaException;
import com.nicolas.flujos.application.port.in.CrearSolicitudCommand;
import com.nicolas.flujos.application.port.in.CrearSolicitudUseCase;
import com.nicolas.flujos.application.port.in.SolicitudDetalle;
import com.nicolas.flujos.application.port.out.IdempotenciaRepository;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CrearSolicitudService implements CrearSolicitudUseCase {

    private final SolicitudRepository solicitudes;
    private final IdempotenciaRepository idempotencia;
    private final Clock clock;
    private final Currency monedaBase;

    CrearSolicitudService(SolicitudRepository solicitudes, IdempotenciaRepository idempotencia, Clock clock,
                          @Value("${app.negocio.moneda-base}") Currency monedaBase) {
        this.solicitudes = solicitudes;
        this.idempotencia = idempotencia;
        this.clock = clock;
        this.monedaBase = monedaBase;
    }

    // La reserva va en la misma transacción que la solicitud: si la creación falla, la clave queda libre.
    @Override
    @Transactional
    public SolicitudDetalle ejecutar(CrearSolicitudCommand c, String clave) {
        if (clave == null) {
            return crear(c);
        }
        var huella = huella(c);
        if (!idempotencia.reservar(clave, c.solicitanteId(), huella)) {
            var previa = idempotencia.buscar(clave, c.solicitanteId()).orElseThrow();
            if (!previa.huella().equals(huella)) {
                throw new ClaveIdempotenciaReutilizadaException();
            }
            return previa.respuesta();
        }
        var detalle = crear(c);
        idempotencia.guardarRespuesta(clave, c.solicitanteId(), detalle);
        return detalle;
    }

    private SolicitudDetalle crear(CrearSolicitudCommand c) {
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

    // Se calcula sobre el comando y no sobre el JSON: el orden de las llaves o escribir 100 en vez de 100.00
    // no cambian lo que se pide. El motivo va al final para que un "|" dentro de él no se confunda con otro campo.
    private static String huella(CrearSolicitudCommand c) {
        var monto = c.monto();
        var periodo = c.periodo();
        return Sha256.de(String.join("|", c.tipo().name(),
            monto == null ? "" : monto.valor().toPlainString(),
            monto == null ? "" : monto.moneda().getCurrencyCode(),
            periodo == null ? "" : periodo.inicio().toString(),
            periodo == null ? "" : periodo.fin().toString(),
            Objects.requireNonNullElse(c.motivo(), "")));
    }
}
