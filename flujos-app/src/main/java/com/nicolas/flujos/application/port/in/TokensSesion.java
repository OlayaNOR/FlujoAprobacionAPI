package com.nicolas.flujos.application.port.in;

import java.time.Duration;

public record TokensSesion(String accessToken, String refreshToken, Duration vigenciaAccess) {
}
