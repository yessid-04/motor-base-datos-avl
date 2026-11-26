package com.motorbd.modelo.indice;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Arbol AVL autobalanceado, implementacion propia.
 *
 * INVARIANTES (se cumplen despues de cada operacion publica):
 *  1. Orden BST: claves del subarbol izquierdo menores que la del nodo,
 *     claves del subarbol derecho mayores. No hay claves repetidas.
 *  2. Altura: altura(nodo) = 1 + max(altura(izq), altura(der)).
 *  3. Balance: |altura(izq) - altura(der)| <= 1 en todos los nodos.
 *
 * Gracias al invariante 3 la altura es O(log n), por eso insertar, buscar
 * y eliminar cuestan O(log n). Cuando una insercion o eliminacion deja un
 * nodo con factor +2 o -2 se aplica una de las cuatro rotaciones (LL, RR,
 * LR, RL) al volver de la recursion.
 *
 * @param <K> tipo de la clave
 * @param <V> tipo del valor
 */
public class AVLArbol<K, V> {

    private final Comparator<? super K> comparador;
    private AVLNodo<K, V> raiz;
    private int tamanio;

    /** Bandera auxiliar para saber si la ultima operacion recursiva cambio algo. */
    private boolean cambio;

    /** Crea un arbol que usa el orden natural de las claves (deben ser Comparable). */
    @SuppressWarnings("unchecked")
    public AVLArbol() {
        this((a, b) -> ((Comparable<Object>) a).compareTo(b));
    }

    /** Crea un arbol con un comparador propio. */
    public AVLArbol(Comparator<? super K> comparador) {
        this.comparador = comparador;
    }

    // ------------------------------------------------------------------
    // Consultas basicas
    // ------------------------------------------------------------------

    public int tamanio() {
        return tamanio;
    }

    public boolean estaVacio() {
        return tamanio == 0;
    }

    /** Altura del arbol (0 si esta vacio). O(1). */
    public int altura() {
        return AVLNodo.alturaDe(raiz);
    }

    /** Raiz del arbol, solo para lectura (la usa la vista para dibujarlo). */
    public AVLNodo<K, V> getRaiz() {
        return raiz;
    }

    /** Busca el valor asociado a la clave. O(log n). Devuelve null si no existe. */
    public V buscar(K clave) {
        AVLNodo<K, V> actual = raiz;
        while (actual != null) {
            int c = comparador.compare(clave, actual.clave);
            if (c == 0) {
                return actual.valor;
            }
            actual = c < 0 ? actual.izquierdo : actual.derecho;
        }
        return null;
    }

    /** O(log n). */
    public boolean contiene(K clave) {
        AVLNodo<K, V> actual = raiz;
        while (actual != null) {
            int c = comparador.compare(clave, actual.clave);
            if (c == 0) {
                return true;
            }
            actual = c < 0 ? actual.izquierdo : actual.derecho;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Insercion
    // ------------------------------------------------------------------

    /**
     * Inserta la pareja clave-valor. Si la clave ya existe no hace nada.
     * O(log n).
     *
     * @return true si se inserto, false si la clave ya existia
     */
    public boolean insertar(K clave, V valor) {
        cambio = false;
        raiz = insertar(raiz, clave, valor);
        if (cambio) {
            tamanio++;
        }
        return cambio;
    }

    private AVLNodo<K, V> insertar(AVLNodo<K, V> nodo, K clave, V valor) {
        if (nodo == null) {
            cambio = true;
            return new AVLNodo<>(clave, valor);
        }
        int c = comparador.compare(clave, nodo.clave);
        if (c < 0) {
            nodo.izquierdo = insertar(nodo.izquierdo, clave, valor);
        } else if (c > 0) {
            nodo.derecho = insertar(nodo.derecho, clave, valor);
        } else {
            return nodo; // clave repetida: no se inserta
        }
        return rebalancear(nodo);
    }

    /**
     * Cambia el valor de una clave que ya existe. O(log n).
     *
     * @return true si la clave existia
     */
    public boolean reemplazar(K clave, V valor) {
        AVLNodo<K, V> actual = raiz;
        while (actual != null) {
            int c = comparador.compare(clave, actual.clave);
            if (c == 0) {
                actual.valor = valor;
                return true;
            }
            actual = c < 0 ? actual.izquierdo : actual.derecho;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Eliminacion
    // ------------------------------------------------------------------

    /**
     * Elimina la clave. O(log n).
     * Si el nodo tiene dos hijos se reemplaza por su sucesor inorden
     * (el minimo del subarbol derecho) y se rebalancea el camino.
     *
     * @return true si la clave existia
     */
    public boolean eliminar(K clave) {
        cambio = false;
        raiz = eliminar(raiz, clave);
        if (cambio) {
            tamanio--;
        }
        return cambio;
    }

    private AVLNodo<K, V> eliminar(AVLNodo<K, V> nodo, K clave) {
        if (nodo == null) {
            return null;
        }
        int c = comparador.compare(clave, nodo.clave);
        if (c < 0) {
            nodo.izquierdo = eliminar(nodo.izquierdo, clave);
        } else if (c > 0) {
            nodo.derecho = eliminar(nodo.derecho, clave);
        } else {
            cambio = true;
            if (nodo.izquierdo == null) {
                return nodo.derecho;
            }
            if (nodo.derecho == null) {
                return nodo.izquierdo;
            }
            AVLNodo<K, V> sucesor = nodo.derecho;
            while (sucesor.izquierdo != null) {
                sucesor = sucesor.izquierdo;
            }
            nodo.clave = sucesor.clave;
            nodo.valor = sucesor.valor;
            nodo.derecho = eliminarMinimo(nodo.derecho);
        }
        return rebalancear(nodo);
    }

    private AVLNodo<K, V> eliminarMinimo(AVLNodo<K, V> nodo) {
        if (nodo.izquierdo == null) {
            return nodo.derecho;
        }
        nodo.izquierdo = eliminarMinimo(nodo.izquierdo);
        return rebalancear(nodo);
    }

    public void limpiar() {
        raiz = null;
        tamanio = 0;
    }

    // ------------------------------------------------------------------
    // Balanceo y rotaciones
    // ------------------------------------------------------------------

    private void actualizarAltura(AVLNodo<K, V> nodo) {
        nodo.altura = 1 + Math.max(AVLNodo.alturaDe(nodo.izquierdo), AVLNodo.alturaDe(nodo.derecho));
    }

    /**
     * Recalcula la altura y, si el factor de balance es +2 o -2,
     * aplica la rotacion que corresponde. O(1).
     */
    private AVLNodo<K, V> rebalancear(AVLNodo<K, V> nodo) {
        actualizarAltura(nodo);
        int balance = nodo.getFactorBalance();

        if (balance > 1) {
            if (nodo.izquierdo.getFactorBalance() < 0) {
                // Caso Izquierda-Derecha: rotacion doble
                nodo.izquierdo = rotarIzquierda(nodo.izquierdo);
            }
            // Caso Izquierda-Izquierda: rotacion simple a la derecha
            return rotarDerecha(nodo);
        }
        if (balance < -1) {
            if (nodo.derecho.getFactorBalance() > 0) {
                // Caso Derecha-Izquierda: rotacion doble
                nodo.derecho = rotarDerecha(nodo.derecho);
            }
            // Caso Derecha-Derecha: rotacion simple a la izquierda
            return rotarIzquierda(nodo);
        }
        return nodo;
    }

    /*
     *        y                x
     *       / \              / \
     *      x   C    -->     A   y
     *     / \                  / \
     *    A   B                B   C
     */
    private AVLNodo<K, V> rotarDerecha(AVLNodo<K, V> y) {
        AVLNodo<K, V> x = y.izquierdo;
        y.izquierdo = x.derecho;
        x.derecho = y;
        actualizarAltura(y);
        actualizarAltura(x);
        return x;
    }

    /*
     *      x                    y
     *     / \                  / \
     *    A   y      -->       x   C
     *       / \              / \
     *      B   C            A   B
     */
    private AVLNodo<K, V> rotarIzquierda(AVLNodo<K, V> x) {
        AVLNodo<K, V> y = x.derecho;
        x.derecho = y.izquierdo;
        y.izquierdo = x;
        actualizarAltura(x);
        actualizarAltura(y);
        return y;
    }

    // ------------------------------------------------------------------
    // Recorridos y rangos
    // ------------------------------------------------------------------

    /** Recorrido inorden: visita las parejas en orden ascendente de clave. O(n). */
    public void recorrerInorden(BiConsumer<K, V> visitante) {
        recorrerInorden(raiz, visitante);
    }

    private void recorrerInorden(AVLNodo<K, V> nodo, BiConsumer<K, V> visitante) {
        if (nodo == null) {
            return;
        }
        recorrerInorden(nodo.izquierdo, visitante);
        visitante.accept(nodo.clave, nodo.valor);
        recorrerInorden(nodo.derecho, visitante);
    }

    /** Valores en orden ascendente de clave. O(n). */
    public List<V> valoresInorden() {
        List<V> lista = new ArrayList<>(tamanio);
        recorrerInorden((k, v) -> lista.add(v));
        return lista;
    }

    /** Claves en orden ascendente. O(n). */
    public List<K> clavesInorden() {
        List<K> lista = new ArrayList<>(tamanio);
        recorrerInorden((k, v) -> lista.add(k));
        return lista;
    }

    /**
     * Valores cuyas claves estan en el rango. Un limite null significa
     * "sin limite" por ese lado. O(log n + m), m = numero de resultados,
     * porque se podan las ramas que quedan fuera del rango.
     */
    public List<V> rango(K minimo, boolean incluirMinimo, K maximo, boolean incluirMaximo) {
        List<V> resultado = new ArrayList<>();
        rango(raiz, minimo, incluirMinimo, maximo, incluirMaximo, resultado);
        return resultado;
    }

    private void rango(AVLNodo<K, V> nodo, K min, boolean incMin, K max, boolean incMax, List<V> resultado) {
        if (nodo == null) {
            return;
        }
        int cMin = min == null ? 1 : comparador.compare(nodo.clave, min);
        int cMax = max == null ? -1 : comparador.compare(nodo.clave, max);

        if (cMin > 0) {
            rango(nodo.izquierdo, min, incMin, max, incMax, resultado);
        }
        boolean dentroMin = cMin > 0 || (cMin == 0 && incMin);
        boolean dentroMax = cMax < 0 || (cMax == 0 && incMax);
        if (dentroMin && dentroMax) {
            resultado.add(nodo.valor);
        }
        if (cMax < 0) {
            rango(nodo.derecho, min, incMin, max, incMax, resultado);
        }
    }

    // ------------------------------------------------------------------
    // Verificacion de invariantes (para pruebas)
    // ------------------------------------------------------------------

    /** Comprueba los tres invariantes en todo el arbol. O(n). */
    public boolean esValido() {
        return validar(raiz, null, null) >= 0 && contar(raiz) == tamanio;
    }

    /** Devuelve la altura real del subarbol, o -1 si algun invariante falla. */
    private int validar(AVLNodo<K, V> nodo, K min, K max) {
        if (nodo == null) {
            return 0;
        }
        if (min != null && comparador.compare(nodo.clave, min) <= 0) {
            return -1;
        }
        if (max != null && comparador.compare(nodo.clave, max) >= 0) {
            return -1;
        }
        int hi = validar(nodo.izquierdo, min, nodo.clave);
        int hd = validar(nodo.derecho, nodo.clave, max);
        if (hi < 0 || hd < 0 || Math.abs(hi - hd) > 1) {
            return -1;
        }
        int h = 1 + Math.max(hi, hd);
        return h == nodo.altura ? h : -1;
    }

    private int contar(AVLNodo<K, V> nodo) {
        return nodo == null ? 0 : 1 + contar(nodo.izquierdo) + contar(nodo.derecho);
    }

    /**
     * Representacion en texto del arbol para la consola.
     * Cada hijo se marca con [I] (izquierdo) o [D] (derecho).
     */
    public String comoTexto(int maxNodos) {
        StringBuilder sb = new StringBuilder();
        int[] contador = {0};
        comoTexto(raiz, "", "", true, sb, contador, maxNodos);
        if (contador[0] >= maxNodos && tamanio > maxNodos) {
            sb.append("... (").append(tamanio - maxNodos).append(" nodos mas)\n");
        }
        return sb.toString();
    }

    private void comoTexto(AVLNodo<K, V> nodo, String prefijo, String lado, boolean esUltimo,
                           StringBuilder sb, int[] contador, int maxNodos) {
        if (nodo == null || contador[0] >= maxNodos) {
            return;
        }
        contador[0]++;
        boolean esRaiz = lado.isEmpty();
        sb.append(prefijo).append(esRaiz ? "" : (esUltimo ? "\\-- " : "+-- ")).append(lado)
          .append(nodo.clave).append(" (h=").append(nodo.altura)
          .append(", fb=").append(nodo.getFactorBalance()).append(")\n");
        String nuevoPrefijo = esRaiz ? "" : prefijo + (esUltimo ? "    " : "|   ");
        boolean tieneDer = nodo.derecho != null;
        if (nodo.izquierdo != null) {
            comoTexto(nodo.izquierdo, nuevoPrefijo, "[I] ", !tieneDer, sb, contador, maxNodos);
        }
        if (tieneDer) {
            comoTexto(nodo.derecho, nuevoPrefijo, "[D] ", true, sb, contador, maxNodos);
        }
    }
}
