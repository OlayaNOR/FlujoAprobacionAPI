package com.nicolas.flujos.api.mapper;

import com.nicolas.flujos.api.dto.CrearSolicitudRequest;
import com.nicolas.flujos.api.dto.EditarSolicitudRequest;
import com.nicolas.flujos.api.dto.MontoRequest;
import com.nicolas.flujos.api.dto.RegistroAuditoriaResponse;
import com.nicolas.flujos.api.dto.SolicitudResponse;
import com.nicolas.flujos.api.dto.SolicitudResumenResponse;
import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.in.CrearSolicitudCommand;
import com.nicolas.flujos.application.port.in.EditarSolicitudCommand;
import com.nicolas.flujos.application.port.in.SolicitudDetalle;
import com.nicolas.flujos.application.port.in.SolicitudResumen;
import com.nicolas.flujos.domain.auditoria.RegistroAuditoria;
import com.nicolas.flujos.domain.shared.Money;
import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import com.nicolas.flujos.domain.solicitud.SolicitudId;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.util.Currency;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SolicitudMapper {

    @Mapping(target = "solicitanteId", source = "actor.id")
    CrearSolicitudCommand aComando(CrearSolicitudRequest request, Actor actor);

    @Mapping(target = "solicitudId", source = "id")
    @Mapping(target = "actor", source = "actor")
    EditarSolicitudCommand aComando(EditarSolicitudRequest request, SolicitudId id, Actor actor);

    SolicitudResponse aResponse(SolicitudDetalle detalle);

    SolicitudResumenResponse aResponse(SolicitudResumen resumen);

    RegistroAuditoriaResponse aResponse(RegistroAuditoria registro);

    default Money money(MontoRequest monto) {
        return monto == null ? null : new Money(monto.valor(), moneda(monto.moneda()));
    }

    // Currency.getInstance lanza IllegalArgumentException con códigos que pasan el patrón pero no existen, como ZZZ
    private static Currency moneda(String codigo) {
        try {
            return Currency.getInstance(codigo);
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("La moneda " + codigo + " no existe");
        }
    }

    default UUID uuid(UsuarioId id) {
        return id == null ? null : id.valor();
    }

    default UUID uuid(SolicitudId id) {
        return id == null ? null : id.valor();
    }
}
