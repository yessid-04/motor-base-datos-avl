package com.motorbd.controlador.comandos;

import com.motorbd.modelo.datos.Condicion;
import com.motorbd.modelo.esquema.Campo;

import java.util.List;
import java.util.Map;

/** Todos los comandos del lenguaje, como records inmutables. */
public final class Comandos {

    private Comandos() {
    }

    /** CREATE TABLE nombre (campo TIPO [PRIMARY KEY], ...) */
    public record CrearTabla(String tabla, List<Campo> campos) implements Comando { }

    /** DROP TABLE [IF EXISTS] nombre */
    public record BorrarTabla(String tabla, boolean siExiste) implements Comando { }

    /** SHOW TABLES */
    public record MostrarTablas() implements Comando { }

    /** DESCRIBE nombre */
    public record Describir(String tabla) implements Comando { }

    /** CREATE INDEX ON tabla (campo) */
    public record CrearIndice(String tabla, String campo) implements Comando { }

    /** DROP INDEX ON tabla (campo) */
    public record BorrarIndice(String tabla, String campo) implements Comando { }

    /** INSERT INTO tabla [(c1, c2)] VALUES (...), (...) */
    public record Insertar(String tabla, List<String> columnas, List<List<Object>> filas) implements Comando { }

    /** SELECT * | c1, c2 FROM tabla [WHERE condicion] */
    public record Seleccionar(String tabla, List<String> columnas, Condicion condicion) implements Comando { }

    /** UPDATE tabla SET c = v, ... [WHERE condicion] */
    public record Actualizar(String tabla, Map<String, Object> cambios, Condicion condicion) implements Comando { }

    /** DELETE FROM tabla [WHERE condicion] */
    public record Eliminar(String tabla, Condicion condicion) implements Comando { }

    /** LOAD tabla "archivo.csv" */
    public record Cargar(String tabla, String ruta) implements Comando { }

    /** SOURCE "script.sql" */
    public record EjecutarScript(String ruta) implements Comando { }

    /** TREE tabla [campo] */
    public record VerArbol(String tabla, String campo) implements Comando { }

    /** HELP */
    public record Ayuda() implements Comando { }

    /** EXIT */
    public record Salir() implements Comando { }
}
