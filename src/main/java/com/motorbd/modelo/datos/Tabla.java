package com.motorbd.modelo.datos;

import com.motorbd.modelo.ErrorMotor;
import com.motorbd.modelo.esquema.Campo;
import com.motorbd.modelo.esquema.Esquema;
import com.motorbd.modelo.indice.AVLArbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tabla en memoria.
 *
 * - Indice primario: AVL  clave primaria -> Fila. Es el almacenamiento principal.
 * - Indices secundarios: un AVL por campo indexado, valor del campo -> lista de claves primarias
 *   (lista ordenada porque varios registros pueden compartir el mismo valor).
 *
 * Esta clase no toca el disco ni valida tipos: eso lo hace BaseDatos antes de llamarla.
 */
public class Tabla {

    /** Compara dos valores del mismo tipo (Integer, Double, String o Boolean). */
    @SuppressWarnings("unchecked")
    public static final Comparator<Object> COMPARADOR = (a, b) -> ((Comparable<Object>) a).compareTo(b);

    private final String nombre;
    private final Esquema esquema;
    private final AVLArbol<Object, Fila> primario = new AVLArbol<>(COMPARADOR);
    private final Map<String, AVLArbol<Object, List<Object>>> secundarios = new LinkedHashMap<>();

    public Tabla(String nombre, Esquema esquema) {
        this.nombre = nombre;
        this.esquema = esquema;
    }

    public String getNombre() {
        return nombre;
    }

    public Esquema getEsquema() {
        return esquema;
    }

    public int tamanio() {
        return primario.tamanio();
    }

    private String pk() {
        return esquema.getPrimario().getNombre();
    }

    // ------------------------------------------------------------------
    // Escritura
    // ------------------------------------------------------------------

    /** Inserta una fila ya validada. O(log n) por cada indice. */
    public void insertar(Fila fila) {
        Object clave = fila.get(pk());
        if (!primario.insertar(clave, fila)) {
            throw new ErrorMotor("Ya existe un registro con " + pk() + " = " + clave + ".");
        }
        for (Map.Entry<String, AVLArbol<Object, List<Object>>> e : secundarios.entrySet()) {
            agregarASecundario(e.getValue(), fila.get(e.getKey()), clave);
        }
    }

    /** Elimina por clave primaria. Devuelve la fila eliminada o null. O(log n) por indice. */
    public Fila eliminarPorClave(Object clave) {
        Fila fila = primario.buscar(clave);
        if (fila == null) {
            return null;
        }
        primario.eliminar(clave);
        for (Map.Entry<String, AVLArbol<Object, List<Object>>> e : secundarios.entrySet()) {
            quitarDeSecundario(e.getValue(), fila.get(e.getKey()), clave);
        }
        return fila;
    }

    /**
     * Reemplaza una fila por otra con la misma clave primaria y
     * actualiza los indices secundarios de los campos que cambiaron.
     */
    public void reemplazar(Fila vieja, Fila nueva) {
        Object clave = vieja.get(pk());
        if (COMPARADOR.compare(clave, nueva.get(pk())) != 0) {
            throw new ErrorMotor("No se permite cambiar la clave primaria.");
        }
        for (Map.Entry<String, AVLArbol<Object, List<Object>>> e : secundarios.entrySet()) {
            Object antes = vieja.get(e.getKey());
            Object despues = nueva.get(e.getKey());
            if (COMPARADOR.compare(antes, despues) != 0) {
                quitarDeSecundario(e.getValue(), antes, clave);
                agregarASecundario(e.getValue(), despues, clave);
            }
        }
        primario.reemplazar(clave, nueva);
    }

    // ------------------------------------------------------------------
    // Indices secundarios
    // ------------------------------------------------------------------

    /** Crea un indice sobre el campo recorriendo todas las filas. O(n log n). */
    public void crearIndice(String campo) {
        Campo c = esquema.campo(campo);
        if (c.esPrimario()) {
            throw new ErrorMotor("'" + c.getNombre() + "' es la clave primaria y ya esta indexada.");
        }
        if (secundarios.containsKey(c.getNombre())) {
            throw new ErrorMotor("Ya existe un indice sobre '" + c.getNombre() + "'.");
        }
        AVLArbol<Object, List<Object>> arbol = new AVLArbol<>(COMPARADOR);
        primario.recorrerInorden((clave, fila) -> agregarASecundario(arbol, fila.get(c.getNombre()), clave));
        secundarios.put(c.getNombre(), arbol);
    }

    public void borrarIndice(String campo) {
        Campo c = esquema.campo(campo);
        if (secundarios.remove(c.getNombre()) == null) {
            throw new ErrorMotor("No existe un indice sobre '" + c.getNombre() + "'.");
        }
    }

    public Set<String> camposIndexados() {
        return Collections.unmodifiableSet(secundarios.keySet());
    }

    public boolean estaIndexado(String campo) {
        return secundarios.containsKey(campo);
    }

    private static void agregarASecundario(AVLArbol<Object, List<Object>> arbol, Object valor, Object clave) {
        List<Object> claves = arbol.buscar(valor);
        if (claves == null) {
            claves = new ArrayList<>(1);
            arbol.insertar(valor, claves);
        }
        // Se mantiene ordenada por clave primaria para que los resultados salgan en orden.
        int posicion = Collections.binarySearch(claves, clave, COMPARADOR);
        claves.add(posicion >= 0 ? posicion : -posicion - 1, clave);
    }

    private static void quitarDeSecundario(AVLArbol<Object, List<Object>> arbol, Object valor, Object clave) {
        List<Object> claves = arbol.buscar(valor);
        if (claves == null) {
            return;
        }
        int posicion = Collections.binarySearch(claves, clave, COMPARADOR);
        if (posicion >= 0) {
            claves.remove(posicion);
        }
        if (claves.isEmpty()) {
            arbol.eliminar(valor);
        }
    }

    // ------------------------------------------------------------------
    // Lectura
    // ------------------------------------------------------------------

    public Fila buscarPorClave(Object clave) {
        return primario.buscar(clave);
    }

    public List<Fila> todas() {
        return primario.valoresInorden();
    }

    /**
     * Busca las filas que cumplen la condicion (ya normalizada) y elige el mejor camino:
     *  - campo = clave primaria -> indice primario, O(log n + m)
     *  - campo con indice       -> indice secundario, O(log n + m log n)
     *  - otro caso              -> recorrido secuencial, O(n)
     * Si la condicion es null devuelve todas las filas en orden de clave.
     */
    public ResultadoBusqueda buscar(Condicion condicion) {
        if (condicion == null) {
            return new ResultadoBusqueda(todas(), "Recorrido inorden completo del indice primario: O(n)");
        }
        String campo = condicion.getCampo();
        Operador op = condicion.getOperador();

        if (op != Operador.DISTINTO && campo.equals(pk())) {
            List<Fila> filas = rangoDe(primario, condicion);
            return new ResultadoBusqueda(filas, "Indice primario AVL sobre '" + campo + "': O(log n + m)");
        }
        if (op != Operador.DISTINTO && secundarios.containsKey(campo)) {
            List<Fila> filas = new ArrayList<>();
            for (List<Object> claves : rangoDe(secundarios.get(campo), condicion)) {
                for (Object clave : claves) {
                    filas.add(primario.buscar(clave));
                }
            }
            return new ResultadoBusqueda(filas, "Indice secundario AVL sobre '" + campo + "': O(log n + m log n)");
        }
        List<Fila> filas = new ArrayList<>();
        for (Fila f : todas()) {
            if (condicion.cumple(f)) {
                filas.add(f);
            }
        }
        String motivo = op == Operador.DISTINTO ? "operador !=" : "campo '" + campo + "' sin indice";
        return new ResultadoBusqueda(filas, "Recorrido secuencial (" + motivo + "): O(n)");
    }

    /** Traduce la condicion a una consulta de rango sobre un AVL. */
    private static <V> List<V> rangoDe(AVLArbol<Object, V> arbol, Condicion c) {
        Object v = c.getValor();
        switch (c.getOperador()) {
            case IGUAL: {
                V encontrado = arbol.buscar(v);
                List<V> lista = new ArrayList<>(1);
                if (encontrado != null) {
                    lista.add(encontrado);
                }
                return lista;
            }
            case MENOR: return arbol.rango(null, false, v, false);
            case MENOR_IGUAL: return arbol.rango(null, false, v, true);
            case MAYOR: return arbol.rango(v, false, null, false);
            case MAYOR_IGUAL: return arbol.rango(v, true, null, false);
            case ENTRE: return arbol.rango(v, true, c.getValor2(), true);
            default: throw new IllegalStateException("Operador no soportado por indice: " + c.getOperador());
        }
    }

    /**
     * Arbol del campo pedido (null = clave primaria). Solo lectura; lo usa la vista para dibujarlo.
     */
    public AVLArbol<Object, ?> arbol(String campo) {
        if (campo == null || esquema.campo(campo).esPrimario()) {
            return primario;
        }
        AVLArbol<Object, List<Object>> a = secundarios.get(esquema.campo(campo).getNombre());
        if (a == null) {
            throw new ErrorMotor("El campo '" + campo + "' no tiene indice.");
        }
        return a;
    }
}
