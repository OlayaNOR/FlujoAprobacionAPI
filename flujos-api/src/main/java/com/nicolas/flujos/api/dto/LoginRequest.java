package com.nicolas.flujos.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 72 es el máximo que BCrypt tiene en cuenta
public record LoginRequest(@Schema(example = "empleado@flujos.test") @NotBlank @Email @Size(max = 255) String email,
                           @NotBlank @Size(max = 72) String password) {
}
