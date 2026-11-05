package com.nicolas.flujos.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nicolas.flujos.application.port.out.FlujoRepository;
import com.nicolas.flujos.domain.flujo.Flujo;
import com.nicolas.flujos.domain.flujo.FlujoId;
import com.nicolas.flujos.domain.flujo.FlujoNoEncontradoException;
import com.nicolas.flujos.domain.flujo.PasoAprobacion;
import com.nicolas.flujos.domain.flujo.RolAprobador;
import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import com.nicolas.flujos.domain.solicitud.TipoSolicitud;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FlujoServicesTest {

    private final FlujoRepository repositorio = mock(FlujoRepository.class);
    private final ConsultarFlujoService consultar = new ConsultarFlujoService(repositorio);
    private final ReemplazarPasosFlujoService reemplazar = new ReemplazarPasosFlujoService(repositorio);

    private final Flujo compra = Flujo.reconstituir(new FlujoId(UUID.randomUUID()), TipoSolicitud.COMPRA, true,
        List.of(new PasoAprobacion(1, RolAprobador.JEFE, null, 48)), 3);

    @Test
    void reemplazarUsaLaLecturaConBloqueoYSubeLaVersion() {
        when(repositorio.buscarParaActualizar(TipoSolicitud.COMPRA)).thenReturn(Optional.of(compra));
        var pasos = List.of(new PasoAprobacion(1, RolAprobador.JEFE, null, 24),
            new PasoAprobacion(2, RolAprobador.GERENTE, new BigDecimal("1000"), 48));

        var detalle = reemplazar.ejecutar(TipoSolicitud.COMPRA, pasos);

        verify(repositorio).guardar(compra);
        verify(repositorio, never()).buscarPorTipo(any());
        assertThat(detalle.version()).isEqualTo(4);
        assertThat(detalle.pasos()).isEqualTo(pasos);
    }

    @Test
    void unosPasosInvalidosNoSeGuardan() {
        when(repositorio.buscarParaActualizar(TipoSolicitud.COMPRA)).thenReturn(Optional.of(compra));

        assertThatThrownBy(() -> reemplazar.ejecutar(TipoSolicitud.COMPRA, List.of()))
            .isInstanceOf(ReglaNegocioException.class);
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void unTipoSinFlujoEsNoEncontrado() {
        when(repositorio.buscarPorTipo(TipoSolicitud.GASTO)).thenReturn(Optional.empty());
        when(repositorio.buscarParaActualizar(TipoSolicitud.GASTO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultar.ejecutar(TipoSolicitud.GASTO)).isInstanceOf(FlujoNoEncontradoException.class);
        assertThatThrownBy(() -> reemplazar.ejecutar(TipoSolicitud.GASTO, compra.getPasos()))
            .isInstanceOf(FlujoNoEncontradoException.class);
    }

    @Test
    void consultarDevuelveLosPasosYLaVersion() {
        when(repositorio.buscarPorTipo(TipoSolicitud.COMPRA)).thenReturn(Optional.of(compra));

        var detalle = consultar.ejecutar(TipoSolicitud.COMPRA);

        assertThat(detalle.version()).isEqualTo(3);
        assertThat(detalle.pasos()).extracting(PasoAprobacion::rolAprobador).containsExactly(RolAprobador.JEFE);
    }
}
