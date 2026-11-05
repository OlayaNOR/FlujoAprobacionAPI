package com.nicolas.flujos.api.dto;

import com.nicolas.flujos.application.port.in.TokensSesion;

public record TokensResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {

    public static TokensResponse de(TokensSesion sesion) {
        return new TokensResponse(sesion.accessToken(), sesion.refreshToken(), "Bearer",
            sesion.vigenciaAccess().toSeconds());
    }
}
