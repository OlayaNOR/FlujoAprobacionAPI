package com.nicolas.flujos.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
class SeguridadConfig {

    @Bean
    SecurityFilterChain cadena(HttpSecurity http, ServicioJwt jwt, RespuestaProblema problemas) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable) // bearer sin cookies, no hay sesión que falsificar
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(new FiltroJwt(jwt), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e
                .authenticationEntryPoint(problemas)
                .accessDeniedHandler(problemas))
            .build();
    }
}
