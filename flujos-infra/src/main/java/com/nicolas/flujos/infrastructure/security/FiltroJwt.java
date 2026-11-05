package com.nicolas.flujos.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

// No es un bean a propósito: Boot lo registraría también como filtro de servlet, fuera de la cadena de seguridad.
class FiltroJwt extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final ServicioJwt jwt;

    FiltroJwt(ServicioJwt jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        var cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            jwt.validar(cabecera.substring(PREFIJO.length())).ifPresent(actor -> {
                var permisos = actor.roles().stream()
                    .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.name()))
                    .toList();
                SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(actor, null, permisos));
            });
        }
        // Un token inválido no corta aquí: la cadena responde 401 si la ruta exige autenticación.
        chain.doFilter(request, response);
    }
}
