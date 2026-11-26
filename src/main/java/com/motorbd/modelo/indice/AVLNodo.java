package com.motorbd.modelo.indice;

/**
 * Nodo de un arbol AVL.
 *
 * Guarda una clave, el valor asociado, los dos hijos y su altura.
 * La altura de una hoja es 1 y la de un hijo nulo se considera 0.
 *
 * @param <K> tipo de la clave
 * @param <V> tipo del valor
 */
public class AVLNodo<K, V> {

    K clave;
    V valor;
    AVLNodo<K, V> izquierdo;
    AVLNodo<K, V> derecho;
    int altura;

    AVLNodo(K clave, V valor) {
        this.clave = clave;
        this.valor = valor;
        this.altura = 1;
    }

    public K getClave() {
        return clave;
    }

    public V getValor() {
        return valor;
    }

    public AVLNodo<K, V> getIzquierdo() {
        return izquierdo;
    }

    public AVLNodo<K, V> getDerecho() {
        return derecho;
    }

    public int getAltura() {
        return altura;
    }

    /** Factor de balance = altura(izquierdo) - altura(derecho). */
    public int getFactorBalance() {
        return alturaDe(izquierdo) - alturaDe(derecho);
    }

    static int alturaDe(AVLNodo<?, ?> nodo) {
        return nodo == null ? 0 : nodo.altura;
    }
}
