package com.nicolas.flujos.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(@NotBlank @Size(max = 100) String refreshToken) {
}
