package com.motorbd.modelo.persistencia;

import com.motorbd.modelo.BaseDatos;
import com.motorbd.modelo.ErrorMotor;
import com.motorbd.modelo.datos.Condicion;
import com.motorbd.modelo.datos.Operador;
import com.motorbd.modelo.esquema.Campo;
import com.motorbd.modelo.esquema.Tipo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenciaTest {

    @TempDir
    Path carpeta;

    private static final List<Campo> CAMPOS = List.of(
            new Campo("id", Tipo.INT, true),
            new Campo("nombre", Tipo.TEXT, false),
            new Campo("nota", Tipo.REAL, false),
            new Campo("activo", Tipo.BOOL, false));

    private BaseDatos crearConDatos() {
        BaseDatos bd = new BaseDatos(carpeta);
        bd.crearTabla("t", CAMPOS);
        bd.crearIndice("t", "nota");
        bd.insertar("t", null, List.of(
                List.of(1, "Ana \"la grande\"", 4.5, true),
                List.of(2, "Luis, el de sistemas", 3.0, false),
                List.of(3, "Ñandú áéí", 3.0, true)));
        return bd;
    }

    @Test
    void recuperacionTrasReinicio() {
        crearConDatos();
        BaseDatos reabierta = new BaseDatos(carpeta);
        assertEquals(3, reabierta.tabla("t").tamanio());
        assertEquals("Ana \"la grande\"", reabierta.tabla("t").buscarPorClave(1).get("nombre"));
        assertEquals("Ñandú áéí", reabierta.tabla("t").buscarPorClave(3).get("nombre"));
        assertEquals(4.5, reabierta.tabla("t").buscarPorClave(1).get("nota"));
        // el indice secundario se reconstruye
        assertTrue(reabierta.tabla("t").estaIndexado("nota"));
        assertEquals(2, reabierta.seleccionar("t", new Condicion("nota", Operador.IGUAL, 3.0)).getFilas().size());
        assertTrue(reabierta.arbol("t", null).esValido());
    }

    @Test
    void actualizacionesYEliminacionesSobrevivenAlReinicio() {
        BaseDatos bd = crearConDatos();
        bd.actualizar("t", Map.of("activo", true), new Condicion("id", Operador.IGUAL, 2));
        bd.eliminar("t", new Condicion("id", Operador.IGUAL, 1));
        BaseDatos reabierta = new BaseDatos(carpeta);
        assertEquals(2, reabierta.tabla("t").tamanio());
        assertEquals(true, reabierta.tabla("t").buscarPorClave(2).get("activo"));
    }

    @Test
    void tablaBorradaNoReapareceAlReiniciar() {
        BaseDatos bd = crearConDatos();
        bd.borrarTabla("t");
        assertFalse(Files.exists(carpeta.resolve("t.json")));
        assertTrue(new BaseDatos(carpeta).listarTablas().isEmpty());
    }

    @Test
    void archivoDanadoNoSeSobrescribe() throws IOException {
        crearConDatos();
        Path archivo = carpeta.resolve("t.json");
        Files.writeString(archivo, "{ \"tabla\": \"t\", \"filas\": [ {\"id\": 1, ", StandardCharsets.UTF_8);
        String antes = Files.readString(archivo);
        assertThrows(ErrorMotor.class, () -> new BaseDatos(carpeta));
        assertEquals(antes, Files.readString(archivo));
    }

    @Test
    void falloAlGuardarDeshaceLaOperacion() {
        crearConDatos();
        Persistencia queFalla = new Persistencia(carpeta) {
            boolean fallar;

            @Override
            public void guardarTabla(com.motorbd.modelo.datos.Tabla tabla) throws IOException {
                if (fallar) {
                    throw new IOException("disco lleno (simulado)");
                }
                super.guardarTabla(tabla);
            }

            { fallar = true; }
        };
        BaseDatos bd = new BaseDatos(queFalla);

        assertThrows(ErrorMotor.class, () -> bd.insertar("t", null, List.of(List.of(9, "X", 1.0, true))));
        assertThrows(ErrorMotor.class,
                () -> bd.actualizar("t", Map.of("nota", 0.5), new Condicion("nota", Operador.IGUAL, 3.0)));
        assertThrows(ErrorMotor.class, () -> bd.eliminar("t", null));

        // memoria intacta
        assertEquals(3, bd.tabla("t").tamanio());
        assertEquals(3.0, bd.tabla("t").buscarPorClave(2).get("nota"));
        assertEquals(2, bd.seleccionar("t", new Condicion("nota", Operador.IGUAL, 3.0)).getFilas().size());
        // disco intacto
        assertEquals(3, new BaseDatos(carpeta).tabla("t").tamanio());
    }

    @Test
    void escrituraAtomicaNoDejaTemporales() throws IOException {
        crearConDatos();
        try (var archivos = Files.list(carpeta)) {
            assertTrue(archivos.noneMatch(p -> p.toString().endsWith(".tmp")));
        }
    }

    @Test
    void temporalHuerfanoSeIgnoraAlCargar() throws IOException {
        crearConDatos();
        Files.writeString(carpeta.resolve("t.json.tmp"), "basura a medio escribir");
        BaseDatos bd = new BaseDatos(carpeta);
        assertEquals(3, bd.tabla("t").tamanio());
        assertFalse(Files.exists(carpeta.resolve("t.json.tmp")));
    }

    @Test
    void jsonIdaYVuelta() {
        String texto = Json.escribir(Map.of("a", List.of(1L, 2.5, "x\ny", true), "b", Map.of()));
        @SuppressWarnings("unchecked")
        Map<String, Object> leido = (Map<String, Object>) Json.leer(texto);
        assertEquals(List.of(1L, 2.5, "x\ny", true), leido.get("a"));
        assertThrows(Json.ErrorJson.class, () -> Json.leer("{\"a\": [1, 2"));
    }
}
