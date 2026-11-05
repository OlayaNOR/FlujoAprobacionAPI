package com.nicolas.flujos.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Las pruebas lo apagan y llaman a los jobs directamente, para que no corran a mitad de una prueba.
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.jobs.habilitados", havingValue = "true", matchIfMissing = true)
class ProgramacionConfig {
}
