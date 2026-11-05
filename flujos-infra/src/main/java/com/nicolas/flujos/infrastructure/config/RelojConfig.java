package com.nicolas.flujos.infrastructure.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RelojConfig {

    // PostgreSQL guarda microsegundos: con más precisión, crear y consultar devolverían fechas distintas
    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
