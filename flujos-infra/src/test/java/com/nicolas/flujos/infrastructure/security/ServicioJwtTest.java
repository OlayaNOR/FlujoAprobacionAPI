package com.nicolas.flujos.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nicolas.flujos.domain.usuario.Rol;
import com.nicolas.flujos.domain.usuario.Usuario;
import com.nicolas.flujos.domain.usuario.UsuarioId;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ServicioJwtTest {

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-bytes!!";
    private static final Instant AHORA = Instant.parse("2026-11-05T15:30:00Z");
    private static final Duration QUINCE_MINUTOS = Duration.ofMinutes(15);

    private final Usuario usuario = new Usuario(UsuarioId.nuevo(), "ana@flujos.test", "Ana",
        Set.of(Rol.EMPLEADO, Rol.APROBADOR), null, null, true);

    private final ServicioJwt servicio = servicioEn(AHORA);

    @Test
    void unTokenEmitidoSeValidaConElMismoActor() {
        var actor = servicio.validar(servicio.emitir(usuario)).orElseThrow();

        assertThat(actor.id()).isEqualTo(usuario.id());
        assertThat(actor.roles()).containsExactlyInAnyOrder(Rol.EMPLEADO, Rol.APROBADOR);
    }

    @Test
    void aceptaUnTokenRecienVencidoDentroDeLaToleranciaDeReloj() {
        var token = servicio.emitir(usuario);

        assertThat(servicioEn(AHORA.plus(QUINCE_MINUTOS).plusSeconds(29)).validar(token)).isPresent();
    }

    @Test
    void rechazaUnTokenVencidoPasadaLaTolerancia() {
        var token = servicio.emitir(usuario);

        assertThat(servicioEn(AHORA.plus(QUINCE_MINUTOS).plusSeconds(31)).validar(token)).isEmpty();
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        assertThat(servicio.validar(token(SECRETO + "-otra", "flujos-api", "flujos-clientes", List.of("ADMIN")))).isEmpty();
    }

    @Test
    void rechazaOtroEmisor() {
        assertThat(servicio.validar(token(SECRETO, "otro-emisor", "flujos-clientes", List.of("ADMIN")))).isEmpty();
    }

    @Test
    void rechazaOtraAudiencia() {
        assertThat(servicio.validar(token(SECRETO, "flujos-api", "otra-audiencia", List.of("ADMIN")))).isEmpty();
    }

    @Test
    void rechazaUnRolDesconocido() {
        assertThat(servicio.validar(token(SECRETO, "flujos-api", "flujos-clientes", List.of("SUPERADMIN")))).isEmpty();
    }

    @Test
    void rechazaUnTokenSinRoles() {
        assertThat(servicio.validar(token(SECRETO, "flujos-api", "flujos-clientes", null))).isEmpty();
    }

    @Test
    void rechazaUnTokenSinFirma() {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        var header = encoder.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        var payload = encoder.encodeToString("""
            {"sub":"%s","roles":["ADMIN"],"iss":"flujos-api","aud":"flujos-clientes"}
            """.formatted(usuario.id().valor()).getBytes(StandardCharsets.UTF_8));

        assertThat(servicio.validar(header + "." + payload + ".")).isEmpty();
    }

    @Test
    void rechazaBasura() {
        assertThat(servicio.validar("esto.no.es-un-jwt")).isEmpty();
    }

    @Test
    void noArrancaConUnSecretoCorto() {
        var propiedades = new JwtProperties("corto", QUINCE_MINUTOS, "flujos-api", "flujos-clientes");

        assertThatThrownBy(() -> new ServicioJwt(propiedades, Clock.systemUTC()))
            .isInstanceOf(WeakKeyException.class);
    }

    private static ServicioJwt servicioEn(Instant instante) {
        var propiedades = new JwtProperties(SECRETO, QUINCE_MINUTOS, "flujos-api", "flujos-clientes");
        return new ServicioJwt(propiedades, Clock.fixed(instante, ZoneOffset.UTC));
    }

    private String token(String secreto, String emisor, String audiencia, List<String> roles) {
        return Jwts.builder()
            .subject(usuario.id().valor().toString())
            .claim("roles", roles)
            .issuer(emisor)
            .audience().add(audiencia).and()
            .expiration(Date.from(AHORA.plus(QUINCE_MINUTOS)))
            .signWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
            .compact();
    }
}
