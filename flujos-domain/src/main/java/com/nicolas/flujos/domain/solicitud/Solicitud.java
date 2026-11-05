package com.nicolas.flujos.domain.solicitud;

import com.nicolas.flujos.domain.flujo.AsignadorAprobador;
import com.nicolas.flujos.domain.flujo.Flujo;
import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.shared.Periodo;
import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import com.nicolas.flujos.domain.solicitud.EventoDominio.SolicitudAprobada;
import com.nicolas.flujos.domain.solicitud.EventoDominio.SolicitudEnviada;
import com.nicolas.flujos.domain.solicitud.EventoDominio.SolicitudEscalada;
import com.nicolas.flujos.domain.solicitud.EventoDominio.SolicitudRechazada;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Solicitud {

    private static final int LARGO_MAXIMO_MOTIVO = 500;
    private static final int SIN_PASO = 0;

    private final SolicitudId id;
    private final UsuarioId solicitanteId;
    private final TipoSolicitud tipo;
    private final Instant creadaEn;
    private final List<EventoDominio> eventos = new ArrayList<>();

    private Money monto;
    private Periodo periodo;
    private String motivo;
    private EstadoSolicitud estado = EstadoSolicitud.BORRADOR;
    private List<PasoAprobacion> pasos = List.of();
    private int pasoActual = SIN_PASO;
    private UsuarioId aprobadorActualId;
    private Instant fechaLimitePaso;
    private Instant actualizadaEn;
    private long version;

    private Solicitud(SolicitudId id, UsuarioId solicitanteId, TipoSolicitud tipo, String motivo, Instant creadaEn) {
        this.id = Objects.requireNonNull(id);
        this.solicitanteId = Objects.requireNonNull(solicitanteId);
        this.tipo = Objects.requireNonNull(tipo);
        this.motivo = validarMotivo(motivo);
        this.creadaEn = Objects.requireNonNull(creadaEn);
        this.actualizadaEn = creadaEn;
    }

    /** Para cargar desde persistencia: no valida fechas porque un periodo ya pasado sigue siendo correcto. */
    public static Solicitud reconstituir(SolicitudId id, UsuarioId solicitanteId, TipoSolicitud tipo,
                                         Money monto, Periodo periodo, String motivo, EstadoSolicitud estado,
                                         List<PasoAprobacion> pasos, int pasoActual, UsuarioId aprobadorActualId,
                                         Instant fechaLimitePaso,
                                         long version, Instant creadaEn, Instant actualizadaEn) {
        var solicitud = new Solicitud(id, solicitanteId, tipo, motivo, creadaEn);
        solicitud.monto = monto;
        solicitud.periodo = periodo;
        solicitud.estado = Objects.requireNonNull(estado);
        solicitud.pasos = List.copyOf(pasos);
        solicitud.pasoActual = pasoActual;
        solicitud.aprobadorActualId = aprobadorActualId;
        solicitud.fechaLimitePaso = fechaLimitePaso;
        solicitud.version = version;
        solicitud.actualizadaEn = actualizadaEn;
        return solicitud;
    }

    public static Solicitud nuevaCompra(UsuarioId solicitanteId, Money monto, String motivo,
                                        Currency monedaBase, Instant ahora) {
        return conMonto(TipoSolicitud.COMPRA, solicitanteId, monto, motivo, monedaBase, ahora);
    }

    public static Solicitud nuevoGasto(UsuarioId solicitanteId, Money monto, String motivo,
                                       Currency monedaBase, Instant ahora) {
        return conMonto(TipoSolicitud.GASTO, solicitanteId, monto, motivo, monedaBase, ahora);
    }

    public static Solicitud nuevasVacaciones(UsuarioId solicitanteId, Periodo periodo, String motivo,
                                             LocalDate hoy, Instant ahora) {
        var solicitud = new Solicitud(SolicitudId.nuevo(), solicitanteId, TipoSolicitud.VACACIONES, motivo, ahora);
        solicitud.periodo = validarPeriodo(periodo, hoy);
        return solicitud;
    }

    private static Solicitud conMonto(TipoSolicitud tipo, UsuarioId solicitanteId, Money monto, String motivo,
                                      Currency monedaBase, Instant ahora) {
        var solicitud = new Solicitud(SolicitudId.nuevo(), solicitanteId, tipo, motivo, ahora);
        solicitud.monto = validarMonto(monto, monedaBase);
        return solicitud;
    }

    public void editarMonto(Money nuevoMonto, String nuevoMotivo, Currency monedaBase, Instant ahora) {
        validarEditable();
        if (tipo == TipoSolicitud.VACACIONES) {
            throw new ReglaNegocioException("Una solicitud de vacaciones no tiene monto");
        }
        this.monto = validarMonto(nuevoMonto, monedaBase);
        this.motivo = validarMotivo(nuevoMotivo);
        this.actualizadaEn = ahora;
    }

    public void editarPeriodo(Periodo nuevoPeriodo, String nuevoMotivo, LocalDate hoy, Instant ahora) {
        validarEditable();
        if (tipo != TipoSolicitud.VACACIONES) {
            throw new ReglaNegocioException("Solo las solicitudes de vacaciones tienen periodo");
        }
        this.periodo = validarPeriodo(nuevoPeriodo, hoy);
        this.motivo = validarMotivo(nuevoMotivo);
        this.actualizadaEn = ahora;
    }

    /** Los pasos del flujo quedan copiados aquí: cambiar el flujo después no afecta a esta solicitud. */
    public void enviar(Flujo flujo, AsignadorAprobador asignador, Instant ahora) {
        validarEstadoOrigen(EstadoSolicitud.BORRADOR, EstadoSolicitud.PENDIENTE);
        this.pasos = flujo.pasosPara(magnitud());

        if (pasos.isEmpty()) {
            finalizar(EstadoSolicitud.APROBADA, ahora);
            eventos.add(new SolicitudAprobada(id, ahora));
            return;
        }

        this.estado = EstadoSolicitud.PENDIENTE;
        avanzarAlPaso(1, asignador, ahora);
        eventos.add(new SolicitudEnviada(id, aprobadorActualId, ahora));
    }

    public void aprobar(UsuarioId aprobador, AsignadorAprobador asignador, Instant ahora) {
        validarTransicion(EstadoSolicitud.APROBADA);
        validarAprobadorDelPaso(aprobador);

        if (pasoActual >= pasos.size()) {
            finalizar(EstadoSolicitud.APROBADA, ahora);
            eventos.add(new SolicitudAprobada(id, ahora));
        } else {
            avanzarAlPaso(pasoActual + 1, asignador, ahora);
            eventos.add(new SolicitudEnviada(id, aprobadorActualId, ahora));
        }
    }

    public void rechazar(UsuarioId aprobador, Instant ahora) {
        validarTransicion(EstadoSolicitud.RECHAZADA);
        validarAprobadorDelPaso(aprobador);
        finalizar(EstadoSolicitud.RECHAZADA, ahora);
        eventos.add(new SolicitudRechazada(id, ahora));
    }

    public void cancelar(UsuarioId actor, Instant ahora) {
        validarTransicion(EstadoSolicitud.CANCELADA);
        if (!solicitanteId.equals(actor)) {
            throw new AccionNoPermitidaException("Solo el solicitante puede cancelar la solicitud");
        }
        // pasoActual se conserva: la auditoría deja constancia del paso en el que se canceló
        this.estado = EstadoSolicitud.CANCELADA;
        this.aprobadorActualId = null;
        this.fechaLimitePaso = null;
        this.actualizadaEn = ahora;
    }

    public void cerrar(Instant ahora) {
        validarTransicion(EstadoSolicitud.CERRADA);
        this.estado = EstadoSolicitud.CERRADA;
        this.actualizadaEn = ahora;
    }

    public void escalar(AsignadorAprobador asignador, Instant ahora) {
        validarEstadoOrigen(EstadoSolicitud.PENDIENTE, EstadoSolicitud.PENDIENTE);
        if (!estaVencida(ahora)) {
            return;
        }

        if (pasoActual < pasos.size()) {
            avanzarAlPaso(pasoActual + 1, asignador, ahora);
            eventos.add(new SolicitudEscalada(id, pasoActual, false, ahora));
        } else {
            // sin fecha límite el job no la vuelve a tomar en cada corrida
            this.fechaLimitePaso = null;
            this.actualizadaEn = ahora;
            eventos.add(new SolicitudEscalada(id, pasoActual, true, ahora));
        }
    }

    public boolean estaVencida(Instant ahora) {
        return estado == EstadoSolicitud.PENDIENTE
            && fechaLimitePaso != null
            && fechaLimitePaso.isBefore(ahora);
    }

    /**
     * Cuándo le llegó el paso actual a su aprobador. Se deduce de la fecha límite, así que queda vacío
     * después de avisar al ADMIN, cuando esa fecha se borra.
     */
    public Optional<Instant> inicioPasoActual() {
        if (estado != EstadoSolicitud.PENDIENTE || fechaLimitePaso == null) {
            return Optional.empty();
        }
        return Optional.of(fechaLimitePaso.minus(Duration.ofHours(pasos.get(pasoActual - 1).tiempoLimiteHoras())));
    }

    public boolean esAprobadorDelPasoActual(UsuarioId usuario) {
        return usuario != null && usuario.equals(aprobadorActualId);
    }

    public List<EventoDominio> pullEventos() {
        var copia = List.copyOf(eventos);
        eventos.clear();
        return copia;
    }

    private void avanzarAlPaso(int numero, AsignadorAprobador asignador, Instant ahora) {
        var paso = pasos.get(numero - 1);
        this.aprobadorActualId = Objects.requireNonNull(asignador.aprobadorPara(paso.rolAprobador(), solicitanteId));
        this.pasoActual = numero;
        this.fechaLimitePaso = ahora.plus(Duration.ofHours(paso.tiempoLimiteHoras()));
        this.actualizadaEn = ahora;
    }

    // Contra lo que se comparan los umbrales del flujo
    private BigDecimal magnitud() {
        return tipo == TipoSolicitud.VACACIONES ? BigDecimal.valueOf(periodo.dias()) : monto.valor();
    }

    private void finalizar(EstadoSolicitud estadoFinal, Instant ahora) {
        this.estado = estadoFinal;
        this.aprobadorActualId = null;
        this.fechaLimitePaso = null;
        this.actualizadaEn = ahora;
    }

    private void validarTransicion(EstadoSolicitud destino) {
        if (!estado.puedeTransicionarA(destino)) {
            throw new TransicionInvalidaException(estado, destino);
        }
    }

    // PENDIENTE es destino válido desde dos estados, así que la tabla no alcanza para enviar y escalar
    private void validarEstadoOrigen(EstadoSolicitud origen, EstadoSolicitud destino) {
        if (estado != origen) {
            throw new TransicionInvalidaException(estado, destino);
        }
    }

    private void validarAprobadorDelPaso(UsuarioId aprobador) {
        if (!esAprobadorDelPasoActual(aprobador)) {
            throw new AccionNoPermitidaException("El usuario no es el aprobador del paso actual");
        }
    }

    private void validarEditable() {
        if (estado != EstadoSolicitud.BORRADOR) {
            throw new SolicitudNoEditableException(estado);
        }
    }

    private static Money validarMonto(Money monto, Currency monedaBase) {
        if (monto == null) {
            throw new ReglaNegocioException("El monto es obligatorio para este tipo de solicitud");
        }
        if (!monto.moneda().equals(monedaBase)) {
            throw new ReglaNegocioException("La moneda debe ser " + monedaBase);
        }
        return monto;
    }

    private static Periodo validarPeriodo(Periodo periodo, LocalDate hoy) {
        if (periodo == null) {
            throw new ReglaNegocioException("El periodo es obligatorio para las vacaciones");
        }
        if (periodo.inicio().isBefore(hoy)) {
            throw new ReglaNegocioException("El periodo no puede empezar en el pasado");
        }
        return periodo;
    }

    private static String validarMotivo(String motivo) {
        if (motivo != null && motivo.length() > LARGO_MAXIMO_MOTIVO) {
            throw new ReglaNegocioException(
                "El motivo no puede superar %d caracteres".formatted(LARGO_MAXIMO_MOTIVO));
        }
        return motivo;
    }

    public SolicitudId getId() {
        return id;
    }

    public UsuarioId getSolicitanteId() {
        return solicitanteId;
    }

    public TipoSolicitud getTipo() {
        return tipo;
    }

    public Money getMonto() {
        return monto;
    }

    public Periodo getPeriodo() {
        return periodo;
    }

    public String getMotivo() {
        return motivo;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public List<PasoAprobacion> getPasos() {
        return pasos;
    }

    public int getPasoActual() {
        return pasoActual;
    }

    public UsuarioId getAprobadorActualId() {
        return aprobadorActualId;
    }

    public Instant getFechaLimitePaso() {
        return fechaLimitePaso;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getActualizadaEn() {
        return actualizadaEn;
    }

    public long getVersion() {
        return version;
    }
}
