package com.nicolas.flujos.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nicolas.flujos.application.port.out.UsuarioRepository;
import com.nicolas.flujos.domain.flujo.RolAprobador;
import com.nicolas.flujos.domain.shared.ReglaNegocioException;
import com.nicolas.flujos.domain.usuario.Rol;
import com.nicolas.flujos.domain.usuario.Usuario;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AsignadorPorJerarquiaTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final AsignadorPorJerarquia asignador = new AsignadorPorJerarquia(usuarios);

    private final UsuarioId solicitanteId = UsuarioId.nuevo();
    private final UsuarioId jefeId = UsuarioId.nuevo();

    @Test
    void elPasoDeJefeVaAlJefeDirectoDelSolicitante() {
        registrarSolicitanteConJefe();
        registrar(jefeId, Set.of(Rol.APROBADOR), true);

        assertThat(asignador.aprobadorPara(RolAprobador.JEFE, solicitanteId)).isEqualTo(jefeId);
    }

    @Test
    void falla422SiElSolicitanteNoTieneJefe() {
        when(usuarios.buscarPorId(solicitanteId)).thenReturn(Optional.of(usuario(solicitanteId, Set.of(Rol.EMPLEADO), null, true)));

        assertThatThrownBy(() -> asignador.aprobadorPara(RolAprobador.JEFE, solicitanteId))
            .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void noAsignaAUnJefeInactivo() {
        registrarSolicitanteConJefe();
        registrar(jefeId, Set.of(Rol.APROBADOR), false);

        assertThatThrownBy(() -> asignador.aprobadorPara(RolAprobador.JEFE, solicitanteId))
            .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void noAsignaAUnJefeSinRolDeAprobador() {
        registrarSolicitanteConJefe();
        registrar(jefeId, Set.of(Rol.EMPLEADO), true);

        assertThatThrownBy(() -> asignador.aprobadorPara(RolAprobador.JEFE, solicitanteId))
            .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void losDemasPasosVanAQuienTengaElCargoSinIncluirAlSolicitante() {
        var gerente = UsuarioId.nuevo();
        when(usuarios.buscarAprobadorConCargo(RolAprobador.GERENTE, solicitanteId)).thenReturn(Optional.of(gerente));

        assertThat(asignador.aprobadorPara(RolAprobador.GERENTE, solicitanteId)).isEqualTo(gerente);
    }

    @Test
    void falla422SiNadieTieneElCargo() {
        when(usuarios.buscarAprobadorConCargo(RolAprobador.DIRECTOR, solicitanteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asignador.aprobadorPara(RolAprobador.DIRECTOR, solicitanteId))
            .isInstanceOf(ReglaNegocioException.class)
            .hasMessageContaining("DIRECTOR");
    }

    private void registrarSolicitanteConJefe() {
        when(usuarios.buscarPorId(solicitanteId))
            .thenReturn(Optional.of(usuario(solicitanteId, Set.of(Rol.EMPLEADO), jefeId, true)));
    }

    private void registrar(UsuarioId id, Set<Rol> roles, boolean activo) {
        when(usuarios.buscarPorId(id)).thenReturn(Optional.of(usuario(id, roles, null, activo)));
    }

    private static Usuario usuario(UsuarioId id, Set<Rol> roles, UsuarioId jefe, boolean activo) {
        return new Usuario(id, id.valor() + "@flujos.test", "Nombre", roles, null, jefe, activo);
    }
}
