package com.nicolas.flujos.infrastructure.config;

import com.nicolas.flujos.domain.regla.ReglaAprobacion;
import com.nicolas.flujos.domain.regla.ReglaCompra;
import com.nicolas.flujos.domain.regla.ReglaGasto;
import com.nicolas.flujos.domain.regla.ReglaVacaciones;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Las reglas están en el dominio, que no conoce Spring: por eso se registran aquí y no con @Component.
@Configuration
class ReglasConfig {

    private final Duration tiempoLimite;

    ReglasConfig(@Value("${app.negocio.tiempo-limite-paso-horas}") int horas) {
        this.tiempoLimite = Duration.ofHours(horas);
    }

    @Bean
    ReglaAprobacion reglaVacaciones() {
        return new ReglaVacaciones(tiempoLimite);
    }

    @Bean
    ReglaAprobacion reglaCompra() {
        return new ReglaCompra(tiempoLimite);
    }

    @Bean
    ReglaAprobacion reglaGasto() {
        return new ReglaGasto(tiempoLimite);
    }
}
