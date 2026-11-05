package com.nicolas.flujos.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.seguridad.login")
record LimiteLoginProperties(@DefaultValue("5") int intentosPorMinutoIp, @DefaultValue("10") int fallosPorHoraEmail) {
}
