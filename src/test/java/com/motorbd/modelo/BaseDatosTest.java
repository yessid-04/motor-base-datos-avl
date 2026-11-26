package com.motorbd.modelo;

import com.motorbd.modelo.datos.Condicion;
import com.motorbd.modelo.datos.Fila;
import com.motorbd.modelo.datos.Operador;
import com.motorbd.modelo.datos.ResultadoBusqueda;
import com.motorbd.modelo.esquema.Campo;
import com.motorbd.modelo.esquema.Tipo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseDatosTest {

    @TempDir
    Path carpeta;

    private BaseDatos bd;

    @BeforeEach
    void preparar() {
        bd = new BaseDatos(carpeta);
        bd.crearTabla("alumnos", List.of(
                new Campo("id", Tipo.INT, true),
                new Campo("nombre", Tipo.TEXT, false),
                new Campo("edad", Tipo.INT, false),
                new Campo("nota", Tipo.REAL, false),
                new Campo("activo", Tipo.BOOL, false)));
        bd.insertar("alumnos", null, List.of(
                List.of(1, "Ana", 20, 4.5, true),
                List.of(2, "Luis", 22, 3.1, true),
                List.of(3, "Eva", 19, 2.8, false),
                List.of(4, "Juan", 22, 4.0, true),
                List.of(5, "Sara", 25, 3.9, false)));
    }

    private List<Object> ids(ResultadoBusqueda r) {
        List<Object> ids = new ArrayList<>();
        for (Fila f : r.getFilas()) {
            ids.add(f.get("id"));
        }
        return ids;
    }

    @Test
    void insertarYBuscarPorClavePrimaria() {
        ResultadoBusqueda r = bd.seleccionar("alumnos", new Condicion("id", Operador.IGUAL, 3));
        assertEquals(1, r.getFilas().size());
        assertEquals("Eva", r.getFilas().get(0).get("nombre"));
        assertTrue(r.getPlan().startsWith("Indice primario"));
    }

    @Test
    void busquedaPorRangoDeClave() {
        ResultadoBusqueda r = bd.seleccionar("alumnos", new Condicion("id", Operador.ENTRE, 2, 4));
        assertEquals(List.of(2, 3, 4), ids(r));
    }

    @Test
    void busquedaPorIgualdadYRangoEnCampoIndexado() {
        bd.crearIndice("alumnos", "edad");
        ResultadoBusqueda igual = bd.seleccionar("alumnos", new Condicion("edad", Operador.IGUAL, 22));
        assertEquals(List.of(2, 4), ids(igual));
        assertTrue(igual.getPlan().startsWith("Indice secundario"));

        ResultadoBusqueda rango = bd.seleccionar("alumnos", new Condicion("edad", Operador.MAYOR_IGUAL, 22));
        assertEquals(3, rango.getFilas().size());
    }

    @Test
    void busquedaSinIndiceUsaRecorrido() {
        ResultadoBusqueda r = bd.seleccionar("alumnos", new Condicion("nombre", Operador.IGUAL, "Luis"));
        assertEquals(List.of(2), ids(r));
        assertTrue(r.getPlan().startsWith("Recorrido secuencial"));
    }

    @Test
    void literalEnteroSeConvierteAReal() {
        ResultadoBusqueda r = bd.seleccionar("alumnos", new Condicion("nota", Operador.MAYOR_IGUAL, 4));
        assertEquals(List.of(1, 4), ids(r));
    }

    @Test
    void actualizarVariasFilasPorCriterio() {
        bd.crearIndice("alumnos", "activo");
        int n = bd.actualizar("alumnos", Map.of("activo", false), new Condicion("edad", Operador.MAYOR_IGUAL, 22));
        assertEquals(3, n);
        assertEquals(List.of(2, 3, 4, 5),
                ids(bd.seleccionar("alumnos", new Condicion("activo", Operador.IGUAL, false))));
        assertEquals(List.of(1), ids(bd.seleccionar("alumnos", new Condicion("activo", Operador.IGUAL, true))));
    }

    @Test
    void actualizarMantieneIndiceSecundario() {
        bd.crearIndice("alumnos", "edad");
        bd.actualizar("alumnos", Map.of("edad", 30), new Condicion("id", Operador.IGUAL, 1));
        assertTrue(bd.seleccionar("alumnos", new Condicion("edad", Operador.IGUAL, 20)).getFilas().isEmpty());
        assertEquals(List.of(1), ids(bd.seleccionar("alumnos", new Condicion("edad", Operador.IGUAL, 30))));
        assertTrue(bd.arbol("alumnos", "edad").esValido());
    }

    @Test
    void eliminarPorClaveYPorCondicion() {
        bd.crearIndice("alumnos", "edad");
        assertEquals(1, bd.eliminar("alumnos", new Condicion("id", Operador.IGUAL, 1)));
        assertEquals(2, bd.eliminar("alumnos", new Condicion("activo", Operador.IGUAL, false)));
        assertEquals(List.of(2, 4), ids(bd.seleccionar("alumnos", null)));
        assertEquals(List.of(2, 4), ids(bd.seleccionar("alumnos", new Condicion("edad", Operador.IGUAL, 22))));
    }

    @Test
    void tipoIncorrectoNoInsertaNada() {
        assertThrows(ErrorMotor.class, () -> bd.insertar("alumnos", null, List.of(
                List.of(10, "Ok", 20, 3.0, true),
                List.of(11, "Mal", "veinte", 3.0, true))));
        assertNull(bd.tabla("alumnos").buscarPorClave(10));
        assertEquals(5, bd.tabla("alumnos").tamanio());
    }

    @Test
    void clavePrimariaRepetidaSeRechaza() {
        assertThrows(ErrorMotor.class, () -> bd.insertar("alumnos", null, List.of(List.of(1, "X", 1, 1.0, true))));
        assertThrows(ErrorMotor.class, () -> bd.insertar("alumnos", null, List.of(
                List.of(9, "X", 1, 1.0, true), List.of(9, "Y", 1, 1.0, true))));
        assertEquals(5, bd.tabla("alumnos").tamanio());
    }

    @Test
    void noSePuedeCambiarLaClavePrimaria() {
        assertThrows(ErrorMotor.class,
                () -> bd.actualizar("alumnos", Map.of("id", 99), new Condicion("id", Operador.IGUAL, 1)));
    }

    @Test
    void insertarConListaDeColumnasEnOtroOrden() {
        bd.insertar("alumnos", List.of("nombre", "id", "activo", "nota", "edad"),
                List.of(List.of("Leo", 6, true, 3.3, 21)));
        assertEquals("Leo", bd.tabla("alumnos").buscarPorClave(6).get("nombre"));
    }

    @Test
    void crearYBorrarTablas() {
        assertThrows(ErrorMotor.class, () -> bd.crearTabla("alumnos", List.of(new Campo("id", Tipo.INT, true))));
        assertThrows(ErrorMotor.class, () -> bd.crearTabla("sinclave", List.of(new Campo("x", Tipo.INT, false))));
        bd.borrarTabla("alumnos");
        assertTrue(bd.listarTablas().isEmpty());
        assertThrows(ErrorMotor.class, () -> bd.tabla("alumnos"));
    }
}
