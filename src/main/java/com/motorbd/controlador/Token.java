package com.motorbd.controlador;

/** Pieza minima del texto de un comando. */
public final class Token {

    public enum Tipo { PALABRA, NUMERO, TEXTO, SIMBOLO, FIN }

    private final Tipo tipo;
    private final String texto;
    private final Object valor;
    private final int posicion;

    public Token(Tipo tipo, String texto, Object valor, int posicion) {
        this.tipo = tipo;
        this.texto = texto;
        this.valor = valor;
        this.posicion = posicion;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getTexto() {
        return texto;
    }

    /** Valor interpretado: Integer/Long/Double para NUMERO, String para TEXTO. */
    public Object getValor() {
        return valor;
    }

    public int getPosicion() {
        return posicion;
    }

    public boolean es(String palabra) {
        return (tipo == Tipo.PALABRA || tipo == Tipo.SIMBOLO) && texto.equalsIgnoreCase(palabra);
    }

    @Override
    public String toString() {
        return tipo + "(" + texto + ")";
    }
}
