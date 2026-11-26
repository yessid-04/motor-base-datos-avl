package com.motorbd.modelo.esquema;

import com.motorbd.modelo.ErrorMotor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Esquema de una tabla: lista ordenada de campos con exactamente una clave primaria.
 */
public final class Esquema {

    private final List<Campo> campos;
    private final Map<String, Campo> porNombre = new LinkedHashMap<>();
    private final Campo primario;

    public Esquema(List<Campo> campos) {
        if (campos == null || campos.isEmpty()) {
            throw new ErrorMotor("Una tabla necesita al menos un campo.");
        }
        Campo pk = null;
        Set<String> vistos = new HashSet<>();
        for (Campo c : campos) {
            Identificadores.validar(c.getNombre(), "campo");
            if (!vistos.add(c.getNombre().toLowerCase())) {
                throw new ErrorMotor("Campo repetido: " + c.getNombre());
            }
            if (c.esPrimario()) {
                if (pk != null) {
                    throw new ErrorMotor("Solo se permite una clave primaria.");
                }
                pk = c;
            }
            porNombre.put(c.getNombre().toLowerCase(), c);
        }
        if (pk == null) {
            throw new ErrorMotor("Falta la clave primaria: marque un campo con PRIMARY KEY.");
        }
        this.campos = Collections.unmodifiableList(new ArrayList<>(campos));
        this.primario = pk;
    }

    public List<Campo> getCampos() {
        return campos;
    }

    public Campo getPrimario() {
        return primario;
    }

    /** Busca un campo por nombre, sin distinguir mayusculas. Lanza error si no existe. */
    public Campo campo(String nombre) {
        Campo c = porNombre.get(nombre.toLowerCase());
        if (c == null) {
            throw new ErrorMotor("El campo '" + nombre + "' no existe.");
        }
        return c;
    }

    public boolean tieneCampo(String nombre) {
        return porNombre.containsKey(nombre.toLowerCase());
    }

    public List<String> nombres() {
        List<String> nombres = new ArrayList<>();
        for (Campo c : campos) {
            nombres.add(c.getNombre());
        }
        return nombres;
    }
}
