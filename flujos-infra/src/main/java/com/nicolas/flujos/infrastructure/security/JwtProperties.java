package com.nicolas.flujos.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.seguridad.jwt")
record JwtProperties(String secreto, Duration expiracionAccess, String emisor, String audiencia) {
}
