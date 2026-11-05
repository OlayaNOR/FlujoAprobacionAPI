package com.nicolas.flujos.infrastructure.security;

import com.nicolas.flujos.application.port.out.VerificadorPassword;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
class PasswordConfig {

    private static final int FACTOR_BCRYPT = 12;

    @Bean
    VerificadorPassword verificadorPassword() {
        var encoder = new BCryptPasswordEncoder(FACTOR_BCRYPT);
        var hashFicticio = encoder.encode("sin-usuario");
        return (password, hash) -> {
            var coincide = encoder.matches(password, hash != null ? hash : hashFicticio);
            return hash != null && coincide;
        };
    }
}
