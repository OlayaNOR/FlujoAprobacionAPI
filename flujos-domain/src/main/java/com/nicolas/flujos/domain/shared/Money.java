package com.nicolas.flujos.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal valor, Currency moneda) {

    public Money {
        Objects.requireNonNull(valor);
        Objects.requireNonNull(moneda);
        // se redondea antes de validar: 0.001 quedaría en 0.00
        valor = valor.setScale(2, RoundingMode.HALF_EVEN);
        if (valor.signum() <= 0) {
            throw new ReglaNegocioException("El monto debe ser mayor que cero");
        }
    }

    public static Money de(String valor, String moneda) {
        return new Money(new BigDecimal(valor), Currency.getInstance(moneda));
    }

    public Money sumar(Money otro) {
        validarMismaMoneda(otro);
        return new Money(valor.add(otro.valor), moneda);
    }

    public boolean esMenorQue(Money otro) {
        validarMismaMoneda(otro);
        return valor.compareTo(otro.valor) < 0;
    }

    public boolean esMayorQue(Money otro) {
        validarMismaMoneda(otro);
        return valor.compareTo(otro.valor) > 0;
    }

    private void validarMismaMoneda(Money otro) {
        if (!moneda.equals(otro.moneda)) {
            throw new ReglaNegocioException(
                "No se pueden operar montos en %s y %s".formatted(moneda, otro.moneda));
        }
    }
}
