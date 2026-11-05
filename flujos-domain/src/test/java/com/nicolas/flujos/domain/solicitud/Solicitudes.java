package com.nicolas.flujos.domain.solicitud;

import com.nicolas.flujos.domain.flujo.AsignadorAprobador;
import com.nicolas.flujos.domain.flujo.Flujo;
import com.nicolas.flujos.domain.flujo.FlujoId;
import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.flujo.RolAprobador;
import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

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

    public static Flujo flujoCon(RolAprobador... roles) {
        var pasos = new ArrayList<PasoAprobacion>();
        for (var rol : roles) {
            pasos.add(new PasoAprobacion(pasos.size() + 1, rol, null, (int) LIMITE.toHours()));
        }
        return flujoCon(pasos);
    }

    public static Flujo flujoCon(List<PasoAprobacion> pasos) {
        return Flujo.reconstituir(new FlujoId(UUID.randomUUID()), TipoSolicitud.COMPRA, true, pasos, 1);
    }

    public static Solicitud compra(String monto) {
        return Solicitud.nuevaCompra(SOLICITANTE, Money.de(monto, "USD"), "Licencias", USD, AHORA);
    }

    public static Solicitud enBorrador() {
        return compra("500");
    }

    public static Solicitud pendiente(Flujo flujo) {
        var solicitud = enBorrador();
        solicitud.enviar(flujo, ASIGNADOR, AHORA);
        solicitud.pullEventos();
        return solicitud;
    }
}
