package com.motorbd.controlador;

import com.motorbd.controlador.comandos.Comando;
import com.motorbd.controlador.comandos.Comandos;
import com.motorbd.modelo.BaseDatos;
import com.motorbd.modelo.ErrorMotor;
import com.motorbd.modelo.datos.Fila;
import com.motorbd.modelo.datos.ResultadoBusqueda;
import com.motorbd.modelo.datos.Tabla;
import com.motorbd.modelo.esquema.Campo;
import com.motorbd.modelo.esquema.Esquema;
import com.motorbd.modelo.indice.AVLArbol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * CONTROLADOR del patron MVC.
 *
 * Recibe el texto que escribe el usuario desde cualquier vista, lo pasa por el
 * Parser, llama al modelo (BaseDatos) y devuelve un Resultado que la vista muestra.
 * No imprime nada ni conoce Swing: por eso sirve igual para la consola y la GUI.
 */
public class Controlador {

    private static final int MAX_ANIDAMIENTO_SCRIPTS = 5;

    private final BaseDatos modelo;
    private int profundidadScript;

    public Controlador(BaseDatos modelo) {
        this.modelo = modelo;
    }

    public BaseDatos getModelo() {
        return modelo;
    }

    /** Ejecuta una linea de texto. Nunca lanza excepciones: los errores vuelven como Resultado.error. */
    public Resultado ejecutar(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            return Resultado.ok("");
        }
        long inicio = System.nanoTime();
        try {
            Comando comando = Parser.parsear(entrada.trim());
            Resultado r = despachar(comando);
            return conTiempo(r, inicio);
        } catch (ErrorMotor e) {
            return Resultado.error("ERROR: " + e.getMessage());
        } catch (RuntimeException e) {
            return Resultado.error("ERROR interno: " + e);
        }
    }

    private Resultado conTiempo(Resultado r, long inicio) {
        if (!r.tieneTabla() || r.debeSalir()) {
            return r;
        }
        double ms = (System.nanoTime() - inicio) / 1_000_000.0;
        return Resultado.consulta(r.getMensaje() + String.format(" (%.3f ms)", ms),
                r.getColumnas(), r.getFilas(), r.getPlan(), r.getTablaAfectada());
    }

    private Resultado despachar(Comando c) {
        if (c instanceof Comandos.CrearTabla ct) {
            modelo.crearTabla(ct.tabla(), ct.campos());
            return Resultado.ok("OK: tabla '" + ct.tabla() + "' creada.", ct.tabla());
        }
        if (c instanceof Comandos.BorrarTabla bt) {
            if (bt.siExiste() && !modelo.existeTabla(bt.tabla())) {
                return Resultado.ok("OK: la tabla '" + bt.tabla() + "' no existia, nada que borrar.");
            }
            modelo.borrarTabla(bt.tabla());
            return Resultado.ok("OK: tabla '" + bt.tabla() + "' eliminada.");
        }
        if (c instanceof Comandos.MostrarTablas) {
            return mostrarTablas();
        }
        if (c instanceof Comandos.Describir d) {
            return describir(d.tabla());
        }
        if (c instanceof Comandos.CrearIndice ci) {
            modelo.crearIndice(ci.tabla(), ci.campo());
            return Resultado.ok("OK: indice AVL creado sobre " + ci.tabla() + "(" + ci.campo() + ").", ci.tabla());
        }
        if (c instanceof Comandos.BorrarIndice bi) {
            modelo.borrarIndice(bi.tabla(), bi.campo());
            return Resultado.ok("OK: indice eliminado de " + bi.tabla() + "(" + bi.campo() + ").", bi.tabla());
        }
        if (c instanceof Comandos.Insertar in) {
            int n = modelo.insertar(in.tabla(), in.columnas(), in.filas());
            return Resultado.ok("OK: " + n + " fila(s) insertada(s).", in.tabla());
        }
        if (c instanceof Comandos.Seleccionar s) {
            return seleccionar(s);
        }
        if (c instanceof Comandos.Actualizar a) {
            int n = modelo.actualizar(a.tabla(), a.cambios(), a.condicion());
            return Resultado.ok("OK: " + n + " fila(s) actualizada(s).", a.tabla());
        }
        if (c instanceof Comandos.Eliminar e) {
            int n = modelo.eliminar(e.tabla(), e.condicion());
            return Resultado.ok("OK: " + n + " fila(s) eliminada(s).", e.tabla());
        }
        if (c instanceof Comandos.Cargar l) {
            long t0 = System.nanoTime();
            int n = modelo.cargarCsv(l.tabla(), Paths.get(l.ruta()));
            double ms = (System.nanoTime() - t0) / 1_000_000.0;
            return Resultado.ok(String.format("OK: %d fila(s) cargada(s) desde %s (%.1f ms).", n, l.ruta(), ms),
                    l.tabla());
        }
        if (c instanceof Comandos.EjecutarScript es) {
            return ejecutarScript(Paths.get(es.ruta()));
        }
        if (c instanceof Comandos.VerArbol va) {
            return verArbol(va.tabla(), va.campo());
        }
        if (c instanceof Comandos.Ayuda) {
            return Resultado.ok(AYUDA);
        }
        if (c instanceof Comandos.Salir) {
            return Resultado.despedida();
        }
        throw new ErrorMotor("Comando no soportado.");
    }

    private Resultado mostrarTablas() {
        List<List<Object>> filas = new ArrayList<>();
        for (Tabla t : modelo.listarTablas()) {
            List<Object> f = new ArrayList<>();
            f.add(t.getNombre());
            f.add(t.tamanio());
            f.add(t.getEsquema().getPrimario().getNombre());
            f.add(String.join(", ", t.camposIndexados()));
            filas.add(f);
        }
        return Resultado.consulta(filas.size() + " tabla(s).",
                List.of("tabla", "registros", "clave_primaria", "indices"), filas, null, null);
    }

    private Resultado describir(String nombre) {
        Tabla t = modelo.tabla(nombre);
        List<List<Object>> filas = new ArrayList<>();
        for (Campo c : t.getEsquema().getCampos()) {
            String indice = c.esPrimario() ? "PRIMARIO" : (t.estaIndexado(c.getNombre()) ? "SECUNDARIO" : "");
            filas.add(List.of(c.getNombre(), c.getTipo().name(), indice));
        }
        return Resultado.consulta("Tabla '" + t.getNombre() + "' con " + t.tamanio() + " registro(s).",
                List.of("campo", "tipo", "indice"), filas, null, t.getNombre());
    }

    private Resultado seleccionar(Comandos.Seleccionar s) {
        Tabla tabla = modelo.tabla(s.tabla());
        Esquema esquema = tabla.getEsquema();
        List<String> columnas = new ArrayList<>();
        if (s.columnas() == null) {
            columnas.addAll(esquema.nombres());
        } else {
            for (String col : s.columnas()) {
                columnas.add(esquema.campo(col).getNombre());
            }
        }
        ResultadoBusqueda rb = modelo.seleccionar(s.tabla(), s.condicion());
        List<List<Object>> filas = new ArrayList<>();
        for (Fila f : rb.getFilas()) {
            List<Object> valores = new ArrayList<>(columnas.size());
            for (String col : columnas) {
                valores.add(f.get(col));
            }
            filas.add(valores);
        }
        return Resultado.consulta(filas.size() + " fila(s).", columnas, filas, rb.getPlan(), tabla.getNombre());
    }

    private Resultado verArbol(String tabla, String campo) {
        AVLArbol<Object, ?> arbol = modelo.arbol(tabla, campo);
        String titulo = "Arbol AVL de " + tabla + (campo == null ? " (clave primaria)" : " (indice " + campo + ")")
                + ": " + arbol.tamanio() + " nodo(s), altura " + arbol.altura()
                + ", valido=" + arbol.esValido() + "\n";
        return Resultado.ok(titulo + arbol.comoTexto(63), tabla);
    }

    /**
     * Ejecuta un archivo de comandos: uno por linea, ';' final opcional,
     * lineas vacias y comentarios (--) se ignoran. Sigue aunque una linea falle
     * y al final informa cuantas fallaron.
     */
    public Resultado ejecutarScript(Path ruta) {
        if (profundidadScript >= MAX_ANIDAMIENTO_SCRIPTS) {
            throw new ErrorMotor("Demasiados SOURCE anidados.");
        }
        List<String> lineas;
        try {
            lineas = Files.readAllLines(ruta, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ErrorMotor("No se pudo leer el script '" + ruta + "': " + e.getMessage());
        }
        profundidadScript++;
        try {
            int ok = 0;
            List<String> errores = new ArrayList<>();
            int numero = 0;
            for (String linea : lineas) {
                numero++;
                String l = linea.strip().replace("\uFEFF", "");
                if (l.isEmpty() || l.startsWith("--") || l.startsWith("#")) {
                    continue;
                }
                Resultado r = ejecutar(l);
                if (r.esExito()) {
                    ok++;
                } else {
                    errores.add("  linea " + numero + ": " + r.getMensaje());
                }
            }
            StringBuilder sb = new StringBuilder("Script " + ruta + ": " + ok + " comando(s) OK, "
                    + errores.size() + " con error.");
            for (String e : errores) {
                sb.append('\n').append(e);
            }
            return errores.isEmpty() ? Resultado.ok(sb.toString()) : Resultado.error(sb.toString());
        } finally {
            profundidadScript--;
        }
    }

    public static final String AYUDA = String.join("\n",
            "Comandos (palabras clave sin distinguir mayusculas, ';' final opcional):",
            "  CREATE TABLE t (id INT PRIMARY KEY, nombre TEXT, nota REAL, activo BOOL)",
            "  DROP TABLE [IF EXISTS] t",
            "  SHOW TABLES",
            "  DESCRIBE t",
            "  CREATE INDEX ON t (campo)        DROP INDEX ON t (campo)",
            "  INSERT INTO t VALUES (1, \"Ana\", 4.5, true), (2, \"Luis\", 3.8, false)",
            "  INSERT INTO t (id, nombre, nota, activo) VALUES (3, \"Eva\", 4.0, true)",
            "  SELECT * FROM t",
            "  SELECT nombre, nota FROM t WHERE id = 1",
            "  SELECT * FROM t WHERE nota BETWEEN 3.5 AND 4.5",
            "  SELECT * FROM t WHERE nombre = \"Ana\"",
            "  UPDATE t SET activo = false WHERE nota < 3.0",
            "  DELETE FROM t WHERE id = 2",
            "  LOAD t \"datasets/mediano.csv\"      (CSV con encabezado)",
            "  SOURCE \"datasets/pequeno.sql\"      (ejecuta un script)",
            "  TREE t [campo]                       (muestra el arbol AVL)",
            "  HELP | EXIT",
            "Operadores WHERE: =  !=  <  <=  >  >=  BETWEEN x AND y",
            "Tipos: INT, TEXT, REAL, BOOL");
}
