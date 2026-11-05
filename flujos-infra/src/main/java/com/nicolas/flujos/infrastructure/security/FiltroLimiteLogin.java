package com.nicolas.flujos.infrastructure.security;

import io.github.bucket4j.TimeMeter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita los intentos de login por IP, cuenten o no como fallidos. Usa la IP de la conexión: detrás de un proxy
 * habría que configurar forward-headers-strategy, y solo si el proxy es de confianza, o cualquiera podría
 * cambiar de IP mandando X-Forwarded-For.
 */
class FiltroLimiteLogin extends OncePerRequestFilter {

    private static final String RUTA = "/api/v1/auth/login";

    private final CubetasPorClave cubetas;
    private final RespuestaProblema problemas;

    FiltroLimiteLogin(int intentosPorMinuto, RespuestaProblema problemas) {
        this.cubetas = new CubetasPorClave(intentosPorMinuto, Duration.ofMinutes(1), TimeMeter.SYSTEM_NANOTIME);
        this.problemas = problemas;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && RUTA.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        var cubeta = cubetas.de(request.getRemoteAddr());
        if (cubeta.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }
        problemas.demasiadosIntentos(request, response, CubetasPorClave.espera(cubeta),
            "Demasiados intentos de login desde esta dirección. Espera antes de volver a intentar.");
    }
}
