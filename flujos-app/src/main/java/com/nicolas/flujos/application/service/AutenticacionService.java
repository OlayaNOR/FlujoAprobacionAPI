package com.nicolas.flujos.application.service;

import com.nicolas.flujos.application.port.in.AutenticacionUseCase;
import com.nicolas.flujos.application.port.in.CredencialesInvalidasException;
import com.nicolas.flujos.application.port.in.RefreshTokenInvalidoException;
import com.nicolas.flujos.application.port.in.TokensSesion;
import com.nicolas.flujos.application.port.out.CuentaUsuario;
import com.nicolas.flujos.application.port.out.EmisorAccessToken;
import com.nicolas.flujos.application.port.out.RefreshToken;
import com.nicolas.flujos.application.port.out.RefreshTokenRepository;
import com.nicolas.flujos.application.port.out.UsuarioRepository;
import com.nicolas.flujos.application.port.out.VerificadorPassword;
import com.nicolas.flujos.domain.usuario.Usuario;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AutenticacionService implements AutenticacionUseCase {

    private static final int BYTES_REFRESH_TOKEN = 32;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final VerificadorPassword passwords;
    private final EmisorAccessToken emisor;
    private final Clock clock;
    private final Duration vigenciaRefresh;

    AutenticacionService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens,
                         VerificadorPassword passwords, EmisorAccessToken emisor, Clock clock,
                         @Value("${app.seguridad.jwt.expiracion-refresh}") Duration vigenciaRefresh) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.passwords = passwords;
        this.emisor = emisor;
        this.clock = clock;
        this.vigenciaRefresh = vigenciaRefresh;
    }

    // Sin @Transactional: BCrypt tarda y no vale la pena tener una conexión tomada mientras tanto.
    @Override
    public TokensSesion iniciarSesion(String email, String password) {
        var cuenta = usuarios.buscarCuentaPorEmail(email.trim().toLowerCase(Locale.ROOT));

        // Se verifica aunque el email no exista, para que el tiempo de respuesta no lo delate.
        var hash = cuenta.map(CuentaUsuario::passwordHash).orElse(null);
        var coincide = passwords.coincide(password, hash);

        if (cuenta.isEmpty() || !coincide || !cuenta.get().usuario().activo()) {
            throw new CredencialesInvalidasException();
        }
        return abrirSesion(cuenta.get().usuario());
    }

    // noRollbackFor: si se detecta reutilización, la revocación tiene que quedar guardada aunque se lance la excepción.
    @Override
    @Transactional(noRollbackFor = RefreshTokenInvalidoException.class)
    public TokensSesion renovar(String refreshToken) {
        var token = refreshTokens.buscarConBloqueo(hash(refreshToken))
            .orElseThrow(RefreshTokenInvalidoException::new);

        if (token.revocado()) {
            refreshTokens.revocarTodos(token.usuarioId());
            throw new RefreshTokenInvalidoException();
        }
        if (token.expiro(clock.instant())) {
            throw new RefreshTokenInvalidoException();
        }

        refreshTokens.revocar(token.id());
        var usuario = usuarios.buscarPorId(token.usuarioId())
            .filter(Usuario::activo)
            .orElseThrow(RefreshTokenInvalidoException::new);
        return abrirSesion(usuario);
    }

    @Override
    @Transactional
    public void cerrarSesion(UsuarioId actor, String refreshToken) {
        refreshTokens.buscarConBloqueo(hash(refreshToken))
            .filter(token -> token.usuarioId().equals(actor))
            .ifPresent(token -> refreshTokens.revocar(token.id()));
    }

    private TokensSesion abrirSesion(Usuario usuario) {
        var refreshToken = generarRefreshToken();
        refreshTokens.guardar(new RefreshToken(UUID.randomUUID(), usuario.id(), hash(refreshToken),
            clock.instant().plus(vigenciaRefresh), false));
        return new TokensSesion(emisor.emitir(usuario), refreshToken, emisor.vigencia());
    }

    private static String generarRefreshToken() {
        var bytes = new byte[BYTES_REFRESH_TOKEN];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
