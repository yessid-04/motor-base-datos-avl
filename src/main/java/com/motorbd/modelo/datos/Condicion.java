package com.motorbd.modelo.datos;

import com.motorbd.modelo.esquema.Campo;
import com.motorbd.modelo.esquema.Esquema;

/**
 * Condicion simple de un WHERE: campo operador valor
 * (o campo BETWEEN valor AND valor2).
 */
public final class Condicion {

    private final String campo;
    private final Operador operador;
    private final Object valor;
    private final Object valor2;

    public Condicion(String campo, Operador operador, Object valor, Object valor2) {
        this.campo = campo;
        this.operador = operador;
        this.valor = valor;
        this.valor2 = valor2;
    }

    public Condicion(String campo, Operador operador, Object valor) {
        this(campo, operador, valor, null);
    }

    public String getCampo() {
        return campo;
    }

    public Operador getOperador() {
        return operador;
    }

    public Object getValor() {
        return valor;
    }

    public Object getValor2() {
        return valor2;
    }

    /**
     * Devuelve una copia con el nombre real del campo y los literales
     * convertidos al tipo del campo (por ejemplo 3 -> 3.0 si el campo es REAL).
     */
    public Condicion normalizar(Esquema esquema) {
        Campo c = esquema.campo(campo);
        Object v1 = c.getTipo().convertir(valor, c.getNombre());
        Object v2 = operador == Operador.ENTRE ? c.getTipo().convertir(valor2, c.getNombre()) : null;
        return new Condicion(c.getNombre(), operador, v1, v2);
    }

    /** Evalua la condicion sobre una fila (usada en el recorrido secuencial). */
    @SuppressWarnings("unchecked")
    public boolean cumple(Fila fila) {
        Comparable<Object> actual = (Comparable<Object>) fila.get(campo);
        int c = actual.compareTo(valor);
        switch (operador) {
            case IGUAL: return c == 0;
            case DISTINTO: return c != 0;
            case MENOR: return c < 0;
            case MENOR_IGUAL: return c <= 0;
            case MAYOR: return c > 0;
            case MAYOR_IGUAL: return c >= 0;
            case ENTRE: return c >= 0 && actual.compareTo(valor2) <= 0;
            default: return false;
        }
    }

    @Override
    public String toString() {
        if (operador == Operador.ENTRE) {
            return campo + " BETWEEN " + valor + " AND " + valor2;
        }
        return campo + " " + operador.getSimbolo() + " " + valor;
    }
}
