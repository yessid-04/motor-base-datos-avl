package com.motorbd.modelo;

import java.util.ArrayList;
import java.util.List;

/** Separador de lineas CSV con soporte para comillas dobles ("a, b" y "" como comilla). */
final class Csv {

    private Csv() {
    }

    static List<String> separar(String linea) {
        List<String> celdas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (entreComillas) {
                if (c == '"') {
                    if (i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                        actual.append('"');
                        i++;
                    } else {
                        entreComillas = false;
                    }
                } else {
                    actual.append(c);
                }
            } else if (c == '"') {
                entreComillas = true;
            } else if (c == ',') {
                celdas.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        celdas.add(actual.toString());
        return celdas;
    }
}
