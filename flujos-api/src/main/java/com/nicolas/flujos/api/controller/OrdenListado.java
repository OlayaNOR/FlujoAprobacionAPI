package com.nicolas.flujos.api.controller;

import com.nicolas.flujos.api.error.ParametroInvalidoException;
import com.nicolas.flujos.application.port.in.CampoOrden;
import java.util.Map;

record OrdenListado(CampoOrden campo, boolean ascendente) {

    // Lista blanca: el valor de sort nunca llega tal cual al ORDER BY
    private static final Map<String, CampoOrden> CAMPOS = Map.of(
        "creadaEn", CampoOrden.CREADA_EN,
        "monto", CampoOrden.MONTO,
        "estado", CampoOrden.ESTADO);

    static OrdenListado de(String sort) {
        var partes = sort.split(",", -1);
        var campo = CAMPOS.get(partes[0]);
        if (campo == null || partes.length > 2) {
            throw new ParametroInvalidoException("sort admite creadaEn, monto o estado, con ,asc o ,desc");
        }
        if (partes.length == 1) {
            return new OrdenListado(campo, true);
        }
        return switch (partes[1]) {
            case "asc" -> new OrdenListado(campo, true);
            case "desc" -> new OrdenListado(campo, false);
            default -> throw new ParametroInvalidoException("La dirección de sort debe ser asc o desc");
        };
    }
}
