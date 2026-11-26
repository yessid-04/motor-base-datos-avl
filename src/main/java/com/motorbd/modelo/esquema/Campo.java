package com.motorbd.modelo.esquema;

/**
 * Columna de una tabla: nombre, tipo y si es la clave primaria.
 */
public final class Campo {

    private final String nombre;
    private final Tipo tipo;
    private final boolean primario;

    public Campo(String nombre, Tipo tipo, boolean primario) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.primario = primario;
    }

    public String getNombre() {
        return nombre;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public boolean esPrimario() {
        return primario;
    }

    @Override
    public String toString() {
        return nombre + " " + tipo + (primario ? " PRIMARY KEY" : "");
    }
}
