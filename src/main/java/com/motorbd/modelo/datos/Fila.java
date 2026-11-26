package com.motorbd.modelo.datos;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un registro de una tabla: nombre de campo -> valor (ya convertido a su tipo).
 */
public final class Fila {

    private final LinkedHashMap<String, Object> valores;

    public Fila(Map<String, Object> valores) {
        this.valores = new LinkedHashMap<>(valores);
    }

    public Object get(String campo) {
        return valores.get(campo);
    }

    void set(String campo, Object valor) {
        valores.put(campo, valor);
    }

    public Map<String, Object> comoMapa() {
        return new LinkedHashMap<>(valores);
    }

    /** Copia independiente, usada para poder deshacer cambios. */
    public Fila copia() {
        return new Fila(valores);
    }

    @Override
    public String toString() {
        return valores.toString();
    }
}
