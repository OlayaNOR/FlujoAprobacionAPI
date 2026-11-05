package com.nicolas.flujos.api;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

final class TokenDePrueba {

    private TokenDePrueba() {
    }

    static String conRoles(String... roles) {
        return firmado(IntegracionBase.SECRETO_JWT, Instant.now().plus(Duration.ofMinutes(5)), roles);
    }

    static String para(UUID usuario, String... roles) {
        return firmado(IntegracionBase.SECRETO_JWT, usuario, Instant.now().plus(Duration.ofMinutes(5)), roles);
    }

    static String vencido() {
        return firmado(IntegracionBase.SECRETO_JWT, Instant.now().minus(Duration.ofHours(1)), "ADMIN");
    }

    static String firmadoConOtraClave() {
        return firmado(IntegracionBase.SECRETO_JWT + "-otra", Instant.now().plus(Duration.ofMinutes(5)), "ADMIN");
    }

    private static String firmado(String secreto, Instant expira, String... roles) {
        return firmado(secreto, UUID.randomUUID(), expira, roles);
    }

    private static String firmado(String secreto, UUID usuario, Instant expira, String... roles) {
        return Jwts.builder()
            .subject(usuario.toString())
            .claim("roles", List.of(roles))
            .issuer("flujos-api")
            .audience().add("flujos-clientes").and()
            .expiration(Date.from(expira))
            .signWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
            .compact();
    }
}
