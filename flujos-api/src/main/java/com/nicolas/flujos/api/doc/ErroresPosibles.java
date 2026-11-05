package com.nicolas.flujos.api.doc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Códigos de error que puede devolver un endpoint, para la documentación.
 * El 401 no hace falta ponerlo: se agrega solo a todo lo que exige token.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ErroresPosibles {

    int[] value();
}
