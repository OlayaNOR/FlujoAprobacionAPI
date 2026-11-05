package com.nicolas.flujos.application.port.out;

import com.nicolas.flujos.application.port.in.FiltroSolicitud;
import com.nicolas.flujos.application.port.in.Pagina;
import com.nicolas.flujos.application.port.in.PaginaSolicitada;
import com.nicolas.flujos.domain.solicitud.Solicitud;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.util.Optional;

public interface SolicitudRepository {

    void guardar(Solicitud solicitud);

    Optional<Solicitud> buscarPorId(SolicitudId id);

    /** Con {@code visiblePara} solo trae las solicitudes de ese usuario o las que tiene por aprobar; null trae todas. */
    Pagina<Solicitud> listar(FiltroSolicitud filtro, UsuarioId visiblePara, PaginaSolicitada pagina);
}
