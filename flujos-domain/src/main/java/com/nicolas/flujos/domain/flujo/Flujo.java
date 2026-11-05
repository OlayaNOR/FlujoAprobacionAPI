package com.nicolas.flujos.domain.flujo;

import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class Flujo {

    private final FlujoId id;
    private final TipoSolicitud tipo;
    private final boolean activo;
    private List<PasoAprobacion> pasos;
    private long version;

    private Flujo(FlujoId id, TipoSolicitud tipo, boolean activo, List<PasoAprobacion> pasos, long version) {
        this.id = Objects.requireNonNull(id);
        this.tipo = Objects.requireNonNull(tipo);
        this.activo = activo;
        this.pasos = validarPasos(tipo, pasos);
        this.version = version;
    }

    public static Flujo reconstituir(FlujoId id, TipoSolicitud tipo, boolean activo, List<PasoAprobacion> pasos,
                                     long version) {
        return new Flujo(id, tipo, activo, pasos, version);
    }

    public void reemplazarPasos(List<PasoAprobacion> nuevos) {
        this.pasos = validarPasos(tipo, nuevos);
        this.version++;
    }

    /**
     * Los pasos que aplican a una solicitud, numerados desde 1 porque el paso actual de la solicitud
     * es una posición en esta lista. Una lista vacía significa que se aprueba sin intervención de nadie.
     *
     * @param magnitud el monto en la moneda base, o los días en vacaciones
     */
    public List<PasoAprobacion> pasosPara(BigDecimal magnitud) {
        if (!activo) {
            throw new ReglaNegocioException("El flujo de %s está desactivado".formatted(tipo));
        }
        var aplicables = new ArrayList<PasoAprobacion>();
        for (var paso : pasos) {
            if (paso.aplicaA(magnitud)) {
                aplicables.add(paso.conOrden(aplicables.size() + 1));
            }
        }
        return List.copyOf(aplicables);
    }

    private static List<PasoAprobacion> validarPasos(TipoSolicitud tipo, List<PasoAprobacion> pasos) {
        if (pasos == null || pasos.isEmpty()) {
            throw new ReglaNegocioException("Un flujo necesita al menos un paso");
        }
        var roles = new HashSet<RolAprobador>();
        for (int i = 0; i < pasos.size(); i++) {
            var paso = pasos.get(i);
            if (paso.orden() != i + 1) {
                throw new ReglaNegocioException("Los pasos deben ir numerados desde 1 y sin huecos");
            }
            if (!roles.add(paso.rolAprobador())) {
                throw new ReglaNegocioException("El rol %s está repetido en el flujo".formatted(paso.rolAprobador()));
            }
            if (tipo == TipoSolicitud.VACACIONES && !esEntero(paso.umbralMinimo())) {
                throw new ReglaNegocioException("En vacaciones el umbral es un número entero de días");
            }
        }
        return List.copyOf(pasos);
    }

    private static boolean esEntero(BigDecimal umbral) {
        return umbral == null || umbral.stripTrailingZeros().scale() <= 0;
    }

    public FlujoId getId() {
        return id;
    }

    public TipoSolicitud getTipo() {
        return tipo;
    }

    public boolean isActivo() {
        return activo;
    }

    public List<PasoAprobacion> getPasos() {
        return pasos;
    }

    public long getVersion() {
        return version;
    }
}
