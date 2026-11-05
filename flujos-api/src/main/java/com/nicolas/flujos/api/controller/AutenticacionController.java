package com.nicolas.flujos.api.controller;

import com.nicolas.flujos.api.dto.LoginRequest;
import com.nicolas.flujos.api.dto.RefreshTokenRequest;
import com.nicolas.flujos.api.dto.TokensResponse;
import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.in.AutenticacionUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class AutenticacionController {

    private final AutenticacionUseCase autenticacion;

    AutenticacionController(AutenticacionUseCase autenticacion) {
        this.autenticacion = autenticacion;
    }

    @PostMapping("/login")
    TokensResponse login(@Valid @RequestBody LoginRequest request) {
        return TokensResponse.de(autenticacion.iniciarSesion(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    TokensResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return TokensResponse.de(autenticacion.renovar(request.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@AuthenticationPrincipal Actor actor, @Valid @RequestBody RefreshTokenRequest request) {
        autenticacion.cerrarSesion(actor.id(), request.refreshToken());
    }
}
