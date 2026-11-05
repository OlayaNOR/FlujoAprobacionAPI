package com.nicolas.flujos.domain.solicitud;

import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.flujo.RolAprobador;
import com.nicolas.flujos.domain.regla.AsignadorAprobador;
import com.nicolas.flujos.domain.regla.ReglaAprobacion;
import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

public final class Solicitudes {

    public static final Instant AHORA = Instant.parse("2026-11-05T15:30:00Z");
    public static final LocalDate HOY = LocalDate.parse("2026-11-05");
    public static final Currency USD = Currency.getInstance("USD");
    public static final Duration LIMITE = Duration.ofHours(48);

    public static final UsuarioId SOLICITANTE = UsuarioId.nuevo();
    public static final UsuarioId JEFE = UsuarioId.nuevo();
    public static final UsuarioId GERENTE = UsuarioId.nuevo();
    public static final UsuarioId OTRO = UsuarioId.nuevo();

    public static final AsignadorAprobador ASIGNADOR = (rol, solicitante) -> switch (rol) {
        case JEFE -> JEFE;
        case GERENTE -> GERENTE;
        case RRHH, DIRECTOR -> OTRO;
    };

    private Solicitudes() {
    }

    public static ReglaAprobacion reglaCon(RolAprobador... roles) {
        return new ReglaAprobacion() {
            @Override
            public TipoSolicitud tipo() {
                return TipoSolicitud.COMPRA;
            }

            @Override
            public List<PasoAprobacion> pasosRequeridos(Solicitud solicitud) {
                return PasoAprobacion.secuencia(LIMITE, roles);
            }

            @Override
            public Duration tiempoLimitePorPaso() {
                return LIMITE;
            }
        };
    }

    public static Solicitud compra(String monto) {
        return Solicitud.nuevaCompra(SOLICITANTE, Money.de(monto, "USD"), "Licencias", USD, AHORA);
    }

    public static Solicitud enBorrador() {
        return compra("500");
    }

    public static Solicitud pendiente(ReglaAprobacion regla) {
        var solicitud = enBorrador();
        solicitud.enviar(regla, ASIGNADOR, AHORA);
        solicitud.pullEventos();
        return solicitud;
    }
}
