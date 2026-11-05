package com.nicolas.flujos.application.port.in;

import java.util.List;
import java.util.function.Function;

public record Pagina<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public <R> Pagina<R> map(Function<T, R> conversion) {
        return new Pagina<>(content.stream().map(conversion).toList(), page, size, totalElements, totalPages);
    }
}
