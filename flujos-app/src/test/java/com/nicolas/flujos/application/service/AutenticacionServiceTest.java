package com.nicolas.flujos.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nicolas.flujos.application.port.in.CredencialesInvalidasException;
import com.nicolas.flujos.application.port.in.RefreshTokenInvalidoException;
import com.nicolas.flujos.application.port.out.CuentaUsuario;
import com.nicolas.flujos.application.port.out.EmisorAccessToken;
import com.nicolas.flujos.application.port.out.RefreshToken;
import com.nicolas.flujos.application.port.out.RefreshTokenRepository;
import com.nicolas.flujos.application.port.out.UsuarioRepository;
import com.nicolas.flujos.application.port.out.VerificadorPassword;
import com.nicolas.flujos.domain.usuario.Rol;
import com.nicolas.flujos.domain.usuario.Usuario;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AutenticacionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T15:30:00Z");
    private static final Duration SIETE_DIAS = Duration.ofDays(7);
    private static final String EMAIL = "ana@flujos.test";

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final VerificadorPassword passwords = mock(VerificadorPassword.class);
    private final EmisorAccessToken emisor = mock(EmisorAccessToken.class);

    private final AutenticacionService servicio = new AutenticacionService(usuarios, refreshTokens, passwords, emisor,
        Clock.fixed(AHORA, ZoneOffset.UTC), SIETE_DIAS);

    private final UsuarioId idAna = UsuarioId.nuevo();
    private final Usuario ana = usuario(true);

    @BeforeEach
    void emisorDeTokens() {
        when(emisor.emitir(any())).thenReturn("jwt");
        when(emisor.vigencia()).thenReturn(Duration.ofMinutes(15));
    }

    @Test
    void loginCorrectoGuardaSoloElHashDelRefreshToken() {
        registrarCuenta(ana);
        when(passwords.coincide("clave", "hash-bcrypt")).thenReturn(true);

        var sesion = servicio.iniciarSesion(EMAIL, "clave");

        var guardado = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).guardar(guardado.capture());
        assertThat(sesion.accessToken()).isEqualTo("jwt");
        assertThat(guardado.getValue().tokenHash()).hasSize(64).isNotEqualTo(sesion.refreshToken());
        assertThat(guardado.getValue().expiraEn()).isEqualTo(AHORA.plus(SIETE_DIAS));
    }

    @Test
    void elEmailSeNormalizaAntesDeBuscar() {
        registrarCuenta(ana);
        when(passwords.coincide("clave", "hash-bcrypt")).thenReturn(true);

        assertThat(servicio.iniciarSesion("  ANA@flujos.TEST ", "clave").accessToken()).isEqualTo("jwt");
    }

    @Test
    void contrasenaIncorrectaNoAbreSesion() {
        registrarCuenta(ana);

        assertThatThrownBy(() -> servicio.iniciarSesion(EMAIL, "mala"))
            .isInstanceOf(CredencialesInvalidasException.class);
        verify(refreshTokens, never()).guardar(any());
    }

    @Test
    void emailInexistenteTambienPasaPorElVerificador() {
        when(usuarios.buscarCuentaPorEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciarSesion("nadie@flujos.test", "clave"))
            .isInstanceOf(CredencialesInvalidasException.class);
        verify(passwords).coincide(eq("clave"), isNull());
    }

    @Test
    void unUsuarioInactivoNoPuedeEntrar() {
        registrarCuenta(usuario(false));
        when(passwords.coincide("clave", "hash-bcrypt")).thenReturn(true);

        assertThatThrownBy(() -> servicio.iniciarSesion(EMAIL, "clave"))
            .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void renovarRevocaElTokenAnteriorYEmiteUnoNuevo() {
        var token = token(AHORA.plusSeconds(60), false);
        when(refreshTokens.buscarConBloqueo(anyString())).thenReturn(Optional.of(token));
        when(usuarios.buscarPorId(ana.id())).thenReturn(Optional.of(ana));

        var sesion = servicio.renovar("anterior");

        verify(refreshTokens).revocar(token.id());
        verify(refreshTokens).guardar(any());
        assertThat(sesion.refreshToken()).isNotEqualTo("anterior");
    }

    @Test
    void reutilizarUnTokenRevocadoCierraTodasLasSesiones() {
        when(refreshTokens.buscarConBloqueo(anyString())).thenReturn(Optional.of(token(AHORA.plusSeconds(60), true)));

        assertThatThrownBy(() -> servicio.renovar("robado")).isInstanceOf(RefreshTokenInvalidoException.class);

        verify(refreshTokens).revocarTodos(ana.id());
        verify(refreshTokens, never()).guardar(any());
    }

    @Test
    void unTokenQueExpiraJustoAhoraYaNoSirve() {
        when(refreshTokens.buscarConBloqueo(anyString())).thenReturn(Optional.of(token(AHORA, false)));

        assertThatThrownBy(() -> servicio.renovar("vencido")).isInstanceOf(RefreshTokenInvalidoException.class);
    }

    @Test
    void unTokenDesconocidoNoSirve() {
        when(refreshTokens.buscarConBloqueo(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.renovar("inventado")).isInstanceOf(RefreshTokenInvalidoException.class);
    }

    @Test
    void unUsuarioDesactivadoNoPuedeRenovar() {
        when(refreshTokens.buscarConBloqueo(anyString())).thenReturn(Optional.of(token(AHORA.plusSeconds(60), false)));
        when(usuarios.buscarPorId(ana.id())).thenReturn(Optional.of(usuario(false)));

        assertThatThrownBy(() -> servicio.renovar("token")).isInstanceOf(RefreshTokenInvalidoException.class);
        verify(refreshTokens, never()).guardar(any());
    }

    @Test
    void cerrarSesionRevocaElTokenDelActor() {
        var token = token(AHORA.plusSeconds(60), false);
        when(refreshTokens.buscarConBloqueo(anyString())).thenReturn(Optional.of(token));

        servicio.cerrarSesion(ana.id(), "token");

        verify(refreshTokens).revocar(token.id());
    }

    @Test
    void cerrarSesionNoTocaElTokenDeOtroUsuario() {
        when(refreshTokens.buscarConBloqueo(anyString())).thenReturn(Optional.of(token(AHORA.plusSeconds(60), false)));

        servicio.cerrarSesion(UsuarioId.nuevo(), "ajeno");

        verify(refreshTokens, never()).revocar(any());
    }

    private void registrarCuenta(Usuario usuario) {
        when(usuarios.buscarCuentaPorEmail(EMAIL)).thenReturn(Optional.of(new CuentaUsuario(usuario, "hash-bcrypt")));
    }

    private RefreshToken token(Instant expiraEn, boolean revocado) {
        return new RefreshToken(UUID.randomUUID(), ana.id(), "h".repeat(64), expiraEn, revocado);
    }

    private Usuario usuario(boolean activo) {
        return new Usuario(idAna, EMAIL, "Ana", Set.of(Rol.EMPLEADO), null, null, activo);
    }
}
