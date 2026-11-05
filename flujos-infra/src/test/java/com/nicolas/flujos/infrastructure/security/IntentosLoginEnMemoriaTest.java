package com.nicolas.flujos.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class IntentosLoginEnMemoriaTest {

    private final RelojManual reloj = new RelojManual();
    private final IntentosLoginEnMemoria intentos = new IntentosLoginEnMemoria(10, reloj);

    @Test
    void bloqueaDespuesDeDiezFallosYAvisaCuantoFalta() {
        fallar("ana@flujos.test", 9);
        assertThat(intentos.bloqueo("ana@flujos.test")).isEmpty();

        fallar("ana@flujos.test", 1);

        // diez por hora: se recupera un intento cada seis minutos
        assertThat(intentos.bloqueo("ana@flujos.test")).contains(Duration.ofMinutes(6));
    }

    @Test
    void elBloqueoSeLevantaConElTiempo() {
        fallar("ana@flujos.test", 10);

        reloj.avanzar(Duration.ofMinutes(6));

        assertThat(intentos.bloqueo("ana@flujos.test")).isEmpty();
    }

    @Test
    void cadaEmailCuentaPorSeparado() {
        fallar("ana@flujos.test", 10);

        assertThat(intentos.bloqueo("luis@flujos.test")).isEmpty();
    }

    @Test
    void limpiarDevuelveTodosLosIntentos() {
        fallar("ana@flujos.test", 9);

        intentos.limpiar("ana@flujos.test");
        fallar("ana@flujos.test", 9);

        assertThat(intentos.bloqueo("ana@flujos.test")).isEmpty();
    }

    @Test
    void retryAfterRedondeaHaciaArriba() {
        assertThat(RespuestaProblema.segundos(Duration.ofMillis(1))).isEqualTo(1);
        assertThat(RespuestaProblema.segundos(Duration.ofMillis(12_001))).isEqualTo(13);
        assertThat(RespuestaProblema.segundos(Duration.ZERO)).isEqualTo(1);
    }

    private void fallar(String email, int veces) {
        for (int i = 0; i < veces; i++) {
            intentos.registrarFallo(email);
        }
    }

    private static final class RelojManual implements TimeMeter {

        private long nanos;

        void avanzar(Duration tiempo) {
            nanos += tiempo.toNanos();
        }

        @Override
        public long currentTimeNanos() {
            return nanos;
        }

        @Override
        public boolean isWallClockBased() {
            return false;
        }
    }
}
