package com.motorbd.modelo.indice;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AVLArbolTest {

    private AVLArbol<Integer, String> arbolCon(int... claves) {
        AVLArbol<Integer, String> a = new AVLArbol<>();
        for (int c : claves) {
            a.insertar(c, "v" + c);
        }
        return a;
    }

    @Test
    void insertarYBuscar() {
        AVLArbol<Integer, String> a = arbolCon(50, 30, 70, 20, 40);
        assertEquals(5, a.tamanio());
        assertEquals("v40", a.buscar(40));
        assertNull(a.buscar(99));
        assertTrue(a.contiene(20));
        assertTrue(a.esValido());
    }

    @Test
    void noAdmiteClavesRepetidas() {
        AVLArbol<Integer, String> a = arbolCon(1, 2, 3);
        assertFalse(a.insertar(2, "otro"));
        assertEquals("v2", a.buscar(2));
        assertEquals(3, a.tamanio());
    }

    @Test
    void rotacionIzquierdaIzquierda() {
        AVLArbol<Integer, String> a = arbolCon(30, 20, 10);
        assertEquals(20, a.getRaiz().getClave());
        assertEquals(2, a.altura());
        assertTrue(a.esValido());
    }

    @Test
    void rotacionDerechaDerecha() {
        AVLArbol<Integer, String> a = arbolCon(10, 20, 30);
        assertEquals(20, a.getRaiz().getClave());
        assertTrue(a.esValido());
    }

    @Test
    void rotacionIzquierdaDerecha() {
        AVLArbol<Integer, String> a = arbolCon(30, 10, 20);
        assertEquals(20, a.getRaiz().getClave());
        assertTrue(a.esValido());
    }

    @Test
    void rotacionDerechaIzquierda() {
        AVLArbol<Integer, String> a = arbolCon(10, 30, 20);
        assertEquals(20, a.getRaiz().getClave());
        assertTrue(a.esValido());
    }

    @Test
    void eliminarHojaNodoConUnHijoYConDosHijos() {
        AVLArbol<Integer, String> a = arbolCon(50, 30, 70, 20, 40, 60, 80, 35);
        assertTrue(a.eliminar(20));            // hoja
        assertTrue(a.eliminar(40));            // un hijo (35)
        assertTrue(a.eliminar(50));            // dos hijos: lo reemplaza el sucesor 60
        assertFalse(a.eliminar(999));
        assertEquals(List.of(30, 35, 60, 70, 80), a.clavesInorden());
        assertEquals(5, a.tamanio());
        assertTrue(a.esValido());
    }

    @Test
    void recorridoInordenOrdenado() {
        AVLArbol<Integer, String> a = arbolCon(5, 3, 8, 1, 4, 7, 9, 2, 6);
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), a.clavesInorden());
    }

    @Test
    void rangosConYSinLimites() {
        AVLArbol<Integer, String> a = new AVLArbol<>();
        for (int i = 1; i <= 100; i++) {
            a.insertar(i, "v" + i);
        }
        assertEquals(11, a.rango(10, true, 20, true).size());
        assertEquals(9, a.rango(10, false, 20, false).size());
        assertEquals(4, a.rango(null, false, 5, false).size());
        assertEquals(5, a.rango(96, true, null, false).size());
        assertEquals(List.of("v10", "v11", "v12"), a.rango(10, true, 12, true));
        assertTrue(a.rango(200, true, 300, true).isEmpty());
    }

    @Test
    void alturaLogaritmicaConInsercionesOrdenadas() {
        // Insertar en orden es el peor caso de un BST sin balance (altura n).
        AVLArbol<Integer, Integer> a = new AVLArbol<>();
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            a.insertar(i, i);
        }
        // Cota teorica del AVL: altura < 1.44 log2(n + 2)
        double cota = 1.44 * (Math.log(n + 2) / Math.log(2));
        assertTrue(a.altura() < cota, "altura " + a.altura() + " supera " + cota);
        assertTrue(a.esValido());
    }

    @Test
    void operacionesAleatoriasContraTreeMap() {
        Random azar = new Random(42);
        AVLArbol<Integer, Integer> a = new AVLArbol<>();
        TreeMap<Integer, Integer> referencia = new TreeMap<>();
        for (int i = 0; i < 20_000; i++) {
            int clave = azar.nextInt(5_000);
            if (azar.nextBoolean()) {
                assertEquals(!referencia.containsKey(clave), a.insertar(clave, i));
                referencia.putIfAbsent(clave, i);
            } else {
                assertEquals(referencia.remove(clave) != null, a.eliminar(clave));
            }
        }
        assertTrue(a.esValido());
        assertEquals(referencia.size(), a.tamanio());
        assertEquals(new ArrayList<>(referencia.keySet()), a.clavesInorden());
    }

    @Test
    void eliminarTodoEnOrdenAleatorio() {
        List<Integer> claves = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            claves.add(i);
        }
        Collections.shuffle(claves, new Random(7));
        AVLArbol<Integer, Integer> a = new AVLArbol<>();
        claves.forEach(c -> a.insertar(c, c));
        Collections.shuffle(claves, new Random(8));
        for (int c : claves) {
            assertTrue(a.eliminar(c));
            assertTrue(a.esValido());
        }
        assertTrue(a.estaVacio());
        assertEquals(0, a.altura());
    }
}
