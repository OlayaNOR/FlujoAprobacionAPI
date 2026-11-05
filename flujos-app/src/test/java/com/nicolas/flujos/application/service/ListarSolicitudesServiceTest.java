package com.nicolas.flujos.application.service;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.in.CampoOrden;
import com.nicolas.flujos.application.port.in.FiltroSolicitud;
import com.nicolas.flujos.application.port.in.Pagina;
import com.nicolas.flujos.application.port.in.PaginaSolicitada;
import com.nicolas.flujos.application.port.out.SolicitudRepository;
import com.nicolas.flujos.domain.usuario.Rol;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class ListarSolicitudesServiceTest {

    private static final FiltroSolicitud FILTRO = new FiltroSolicitud(Set.of(), null, null, null);
    private static final PaginaSolicitada PAGINA = new PaginaSolicitada(0, 20, CampoOrden.CREADA_EN, false);

    private final SolicitudRepository repositorio = mock(SolicitudRepository.class);
    private final ListarSolicitudesService servicio = new ListarSolicitudesService(repositorio);

    @BeforeEach
    void respuestaVacia() {
        when(repositorio.listar(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(new Pagina<>(List.of(), 0, 20, 0, 0));
    }

    @Test
    void unEmpleadoSoloConsultaLoQueLeCorresponde() {
        var ana = UsuarioId.nuevo();

        servicio.ejecutar(FILTRO, PAGINA, new Actor(ana, Set.of(Rol.EMPLEADO)));

        verify(repositorio).listar(FILTRO, ana, PAGINA);
    }

    @Test
    void unAdminConsultaSinRestriccion() {
        servicio.ejecutar(FILTRO, PAGINA, new Actor(UsuarioId.nuevo(), Set.of(Rol.ADMIN)));

        verify(repositorio).listar(ArgumentMatchers.eq(FILTRO), isNull(), ArgumentMatchers.eq(PAGINA));
    }
}
