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
@EnableConfigurationProperties({JwtProperties.class, LimiteLoginProperties.class})
class SeguridadConfig {

    @Bean
    SecurityFilterChain cadena(HttpSecurity http, ServicioJwt jwt, RespuestaProblema problemas,
                               LimiteLoginProperties limites) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable) // bearer sin cookies, no hay sesión que falsificar
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(new FiltroLimiteLogin(limites.intentosPorMinutoIp(), problemas),
                UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new FiltroJwt(jwt), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e
                .authenticationEntryPoint(problemas)
                .accessDeniedHandler(problemas))
            .build();
    }
}
