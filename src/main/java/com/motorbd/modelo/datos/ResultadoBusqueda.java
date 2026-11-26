package com.motorbd.modelo.datos;

import java.util.List;

/**
 * Filas encontradas y el "plan" usado para encontrarlas
 * (indice primario, indice secundario o recorrido completo).
 */
public final class ResultadoBusqueda {

    private final List<Fila> filas;
    private final String plan;

    public ResultadoBusqueda(List<Fila> filas, String plan) {
        this.filas = filas;
        this.plan = plan;
    }

    public List<Fila> getFilas() {
        return filas;
    }

    public String getPlan() {
        return plan;
    }
}
