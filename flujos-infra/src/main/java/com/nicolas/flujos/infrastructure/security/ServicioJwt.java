package com.nicolas.flujos.infrastructure.security;

import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.out.EmisorAccessToken;
import com.nicolas.flujos.domain.usuario.Rol;
import com.nicolas.flujos.domain.usuario.Usuario;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
class ServicioJwt implements EmisorAccessToken {

    private static final long TOLERANCIA_RELOJ_SEGUNDOS = 30;
    private static final String CLAIM_ROLES = "roles";

    private final JwtProperties propiedades;
    private final Clock clock;
    private final SecretKey clave;
    private final JwtParser parser;

    ServicioJwt(JwtProperties propiedades, Clock clock) {
        this.propiedades = propiedades;
        this.clock = clock;
        // hmacShaKeyFor falla al arrancar si el secreto tiene menos de 256 bits
        this.clave = Keys.hmacShaKeyFor(propiedades.secreto().getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser()
            .verifyWith(clave)
            .requireIssuer(propiedades.emisor())
            .requireAudience(propiedades.audiencia())
            .clockSkewSeconds(TOLERANCIA_RELOJ_SEGUNDOS)
            .clock(() -> Date.from(clock.instant()))
            .build();
    }

    @Override
    public String emitir(Usuario usuario) {
        var ahora = clock.instant();
        return Jwts.builder()
            .subject(usuario.id().valor().toString())
            .claim(CLAIM_ROLES, usuario.roles().stream().map(Rol::name).sorted().toList())
            .issuer(propiedades.emisor())
            .audience().add(propiedades.audiencia()).and()
            .issuedAt(Date.from(ahora))
            .expiration(Date.from(ahora.plus(propiedades.expiracionAccess())))
            .signWith(clave, Jwts.SIG.HS256)
            .compact();
    }

    @Override
    public Duration vigencia() {
        return propiedades.expiracionAccess();
    }

    Optional<Actor> validar(String token) {
        try {
            var claims = parser.parseSignedClaims(token).getPayload();
            var nombresRoles = claims.get(CLAIM_ROLES, List.class);
            if (nombresRoles == null || claims.getSubject() == null) {
                return Optional.empty();
            }
            var roles = new HashSet<Rol>();
            for (var rol : nombresRoles) {
                roles.add(Rol.valueOf(String.valueOf(rol)));
            }
            return Optional.of(new Actor(new UsuarioId(UUID.fromString(claims.getSubject())), roles));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
