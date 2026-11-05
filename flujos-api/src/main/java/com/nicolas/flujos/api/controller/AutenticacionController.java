package com.nicolas.flujos.api.controller;

import com.nicolas.flujos.api.doc.ErroresPosibles;
import com.nicolas.flujos.api.dto.LoginRequest;
import com.nicolas.flujos.api.dto.RefreshTokenRequest;
import com.nicolas.flujos.api.dto.TokensResponse;
import com.nicolas.flujos.application.port.in.Actor;
import com.nicolas.flujos.application.port.in.AutenticacionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Autenticación")
class AutenticacionController {

    private final AutenticacionUseCase autenticacion;

    AutenticacionController(AutenticacionUseCase autenticacion) {
        this.autenticacion = autenticacion;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Inicia sesión",
        description = "Entrega un access token de 15 minutos y un refresh token de 7 días. "
            + "Acepta 5 intentos por minuto por IP y bloquea el email después de 10 fallos en una hora.")
    @ErroresPosibles({400, 401, 429})
    TokensResponse login(@Valid @RequestBody LoginRequest request) {
        return TokensResponse.de(autenticacion.iniciarSesion(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renueva los tokens",
        description = "El refresh token usado queda invalidado. Usarlo otra vez cierra todas las sesiones del usuario.")
    @ErroresPosibles({400, 401})
    TokensResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return TokensResponse.de(autenticacion.renovar(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cierra la sesión del refresh token")
    @ErroresPosibles(400)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@AuthenticationPrincipal Actor actor, @Valid @RequestBody RefreshTokenRequest request) {
        autenticacion.cerrarSesion(actor.id(), request.refreshToken());
    }
}
