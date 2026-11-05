package com.nicolas.flujos.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RelojConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
