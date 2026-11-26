package com.motorbd.controlador;

import com.motorbd.controlador.comandos.Comandos;
import com.motorbd.modelo.BaseDatos;
import com.motorbd.modelo.ErrorMotor;
import com.motorbd.modelo.datos.Operador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControladorTest {

    @TempDir
    Path carpeta;

    private Controlador ctl;

    @BeforeEach
    void preparar() {
        ctl = new Controlador(new BaseDatos(carpeta));
        ok("CREATE TABLE alumnos (id INT PRIMARY KEY, nombre TEXT, edad INT, nota REAL, activo BOOL)");
        ok("INSERT INTO alumnos VALUES (1, \"Ana\", 20, 4.5, true), (2, 'Luis', 22, 3.1, true), "
                + "(3, \"Eva\", 19, 2.8, false)");
    }

    private Resultado ok(String comando) {
        Resultado r = ctl.ejecutar(comando);
        assertTrue(r.esExito(), comando + " -> " + r.getMensaje());
        return r;
    }

    // ---------------- Parser ----------------

    @Test
    void parserCreateTable() {
        Comandos.CrearTabla c = (Comandos.CrearTabla) Parser.parsear(
                "create table t (id int primary key, n text, r real, b bool);");
        assertEquals("t", c.tabla());
        assertEquals(4, c.campos().size());
        assertTrue(c.campos().get(0).esPrimario());
    }

    @Test
    void parserSelectConBetween() {
        Comandos.Seleccionar s = (Comandos.Seleccionar) Parser.parsear(
                "SELECT nombre, edad FROM alumnos WHERE edad BETWEEN 18 AND 21");
        assertEquals(List.of("nombre", "edad"), s.columnas());
        assertEquals(Operador.ENTRE, s.condicion().getOperador());
        assertEquals(18, s.condicion().getValor());
        assertEquals(21, s.condicion().getValor2());
    }

    @Test
    void parserLiterales() {
        Comandos.Insertar i = (Comandos.Insertar) Parser.parsear(
                "INSERT INTO t VALUES (-5, 3.25, \"di \"\"hola\"\"\", false)");
        assertEquals(List.of(-5, 3.25, "di \"hola\"", false), i.filas().get(0));
    }

    @Test
    void parserErrores() {
        assertThrows(ErrorMotor.class, () -> Parser.parsear("SELEC * FROM t"));
        assertThrows(ErrorMotor.class, () -> Parser.parsear("SELECT * FROM"));
        assertThrows(ErrorMotor.class, () -> Parser.parsear("INSERT INTO t VALUES (1, hola)"));
        assertThrows(ErrorMotor.class, () -> Parser.parsear("SELECT * FROM t WHERE a = \"sin cerrar"));
        assertThrows(ErrorMotor.class, () -> Parser.parsear("DELETE FROM t WHERE a ~ 3"));
    }

    @Test
    void lexerTokens() {
        List<Token> t = Lexer.tokenizar("a>=3 AND b<>'x'");
        assertEquals(List.of("a", ">=", "3", "AND", "b", "<>", "'x'", ""),
                t.stream().map(Token::getTexto).toList());
    }

    // ---------------- Comandos completos ----------------

    @Test
    void selectDevuelveFilasYPlan() {
        ok("CREATE INDEX ON alumnos (edad)");
        Resultado r = ok("SELECT nombre FROM alumnos WHERE edad >= 20");
        assertEquals(List.of("nombre"), r.getColumnas());
        assertEquals(List.of(List.of("Ana"), List.of("Luis")), r.getFilas());
        assertTrue(r.getPlan().contains("secundario"));
    }

    @Test
    void updateYDeleteConCondicion() {
        assertTrue(ok("UPDATE alumnos SET activo = false, nota = 1.0 WHERE edad > 19").getMensaje()
                .contains("2 fila(s)"));
        assertTrue(ok("DELETE FROM alumnos WHERE activo = false").getMensaje().contains("3 fila(s)"));
        assertTrue(ok("SELECT * FROM alumnos").getFilas().isEmpty());
    }

    @Test
    void erroresNoRompenNada() {
        assertFalse(ctl.ejecutar("INSERT INTO alumnos VALUES (4, \"X\", \"diez\", 1.0, true)").esExito());
        assertFalse(ctl.ejecutar("INSERT INTO alumnos VALUES (1, \"X\", 10, 1.0, true)").esExito());
        assertFalse(ctl.ejecutar("SELECT * FROM noexiste").esExito());
        assertFalse(ctl.ejecutar("SELECT * FROM alumnos WHERE nada = 1").esExito());
        assertEquals(3, ok("SELECT * FROM alumnos").getFilas().size());
    }

    @Test
    void dropTableIfExists() {
        ok("DROP TABLE IF EXISTS nunca_existio");
        ok("DROP TABLE alumnos");
        assertFalse(ctl.ejecutar("DROP TABLE alumnos").esExito());
    }

    @Test
    void showDescribeTreeHelpExit() {
        assertEquals(1, ok("SHOW TABLES").getFilas().size());
        assertEquals(5, ok("DESCRIBE alumnos").getFilas().size());
        assertTrue(ok("TREE alumnos").getMensaje().contains("valido=true"));
        assertTrue(ok("HELP").getMensaje().contains("CREATE TABLE"));
        assertTrue(ctl.ejecutar("EXIT").debeSalir());
        assertNull(ok("SHOW TABLES").getPlan());
    }

    @Test
    void cargarDatasetsDelProyecto() throws Exception {
        // Ejecuta los datasets reales del repositorio (se corre desde la raiz del proyecto)
        Path pequeno = Paths.get("datasets/pequeno.sql");
        Path mediano = Paths.get("datasets/cargar_mediano.sql");
        if (!Files.exists(pequeno) || !Files.exists(mediano)) {
            return;
        }
        assertTrue(ctl.ejecutarScript(pequeno).esExito());
        assertEquals(20, ctl.getModelo().tabla("estudiantes").tamanio());
        assertTrue(ctl.ejecutarScript(mediano).esExito());
        assertEquals(10_000, ctl.getModelo().tabla("estudiantes_m").tamanio());
        Resultado r = ok("SELECT * FROM estudiantes_m WHERE id = 7777");
        assertEquals(1, r.getFilas().size());
        assertTrue(ctl.getModelo().arbol("estudiantes_m", null).esValido());
        assertTrue(ctl.getModelo().arbol("estudiantes_m", null).altura() <= 20);
        // y sobrevive a un reinicio
        BaseDatos reabierta = new BaseDatos(carpeta);
        assertEquals(10_000, reabierta.tabla("estudiantes_m").tamanio());
    }

    @Test
    void scriptConErroresInformaLasLineas() throws Exception {
        Path script = carpeta.resolve("s.sql");
        Files.writeString(script, "-- comentario\nINSERT INTO alumnos VALUES (9, \"Z\", 30, 3.0, true)\nMAL\n");
        Resultado r = ctl.ejecutarScript(script);
        assertFalse(r.esExito());
        assertTrue(r.getMensaje().contains("1 comando(s) OK, 1 con error"));
        assertTrue(r.getMensaje().contains("linea 3"));
        assertInstanceOf(Comandos.Salir.class, Parser.parsear("exit"));
    }
}
