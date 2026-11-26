package com.motorbd.modelo.datos;

/** Operadores permitidos en un WHERE. */
public enum Operador {
    IGUAL("="), DISTINTO("!="), MENOR("<"), MENOR_IGUAL("<="), MAYOR(">"), MAYOR_IGUAL(">="), ENTRE("BETWEEN");

    private final String simbolo;

    Operador(String simbolo) {
        this.simbolo = simbolo;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public static Operador desdeSimbolo(String s) {
        for (Operador o : values()) {
            if (o.simbolo.equalsIgnoreCase(s)) {
                return o;
            }
        }
        if (s.equals("<>")) {
            return DISTINTO;
        }
        return null;
    }
}
