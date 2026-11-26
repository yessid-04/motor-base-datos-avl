package com.motorbd.controlador;

import com.motorbd.controlador.comandos.Comando;
import com.motorbd.controlador.comandos.Comandos;
import com.motorbd.modelo.ErrorMotor;
import com.motorbd.modelo.datos.Condicion;
import com.motorbd.modelo.datos.Operador;
import com.motorbd.modelo.esquema.Campo;
import com.motorbd.modelo.esquema.Tipo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analizador sintactico descendente recursivo.
 * Recibe los tokens del Lexer y construye el Comando correspondiente.
 *
 * Gramatica (palabras clave sin distinguir mayusculas, ';' final opcional):
 *
 *   comando     := crear | borrar | SHOW TABLES | DESCRIBE id | insertar | seleccionar
 *                | actualizar | eliminar | LOAD id texto | SOURCE texto | TREE id [id] | HELP | EXIT
 *   crear       := CREATE TABLE id '(' defCampo (',' defCampo)* ')'
 *                | CREATE INDEX ON id '(' id ')'
 *   defCampo    := id tipo [PRIMARY KEY]
 *   borrar      := DROP TABLE [IF EXISTS] id | DROP INDEX ON id '(' id ')'
 *   insertar    := INSERT INTO id ['(' id (',' id)* ')'] VALUES tupla (',' tupla)*
 *   tupla       := '(' literal (',' literal)* ')'
 *   seleccionar := SELECT ('*' | id (',' id)*) FROM id [WHERE condicion]
 *   actualizar  := UPDATE id SET id '=' literal (',' id '=' literal)* [WHERE condicion]
 *   eliminar    := DELETE FROM id [WHERE condicion]
 *   condicion   := id op literal | id BETWEEN literal AND literal
 *   op          := '=' | '!=' | '<>' | '<' | '<=' | '>' | '>='
 *   literal     := numero | texto | TRUE | FALSE
 */
public final class Parser {

    private final List<Token> tokens;
    private int pos;

    private Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public static Comando parsear(String entrada) {
        Parser p = new Parser(Lexer.tokenizar(entrada));
        Comando c = p.comando();
        if (p.actual().es(";")) {
            p.pos++;
        }
        if (p.actual().getTipo() != Token.Tipo.FIN) {
            throw p.error("Sobra texto: '" + p.actual().getTexto() + "'");
        }
        return c;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private Token actual() {
        return tokens.get(pos);
    }

    private Token avanzar() {
        return tokens.get(pos++);
    }

    private boolean acepta(String palabra) {
        if (actual().es(palabra)) {
            pos++;
            return true;
        }
        return false;
    }

    private void esperar(String palabra) {
        if (!acepta(palabra)) {
            throw error("Se esperaba '" + palabra + "'");
        }
    }

    private String identificador(String que) {
        Token t = actual();
        if (t.getTipo() != Token.Tipo.PALABRA) {
            throw error("Se esperaba el nombre de " + que);
        }
        pos++;
        return t.getTexto();
    }

    private String texto(String que) {
        Token t = actual();
        if (t.getTipo() != Token.Tipo.TEXTO) {
            throw error("Se esperaba " + que + " entre comillas");
        }
        pos++;
        return (String) t.getValor();
    }

    private ErrorMotor error(String mensaje) {
        Token t = actual();
        String donde = t.getTipo() == Token.Tipo.FIN ? "al final del comando" : "en la posicion " + t.getPosicion();
        return new ErrorMotor("Error de sintaxis " + donde + ": " + mensaje + ". Escriba HELP para ver los comandos.");
    }

    // ------------------------------------------------------------------
    // Reglas
    // ------------------------------------------------------------------

    private Comando comando() {
        Token t = actual();
        if (t.getTipo() != Token.Tipo.PALABRA) {
            throw error("El comando debe empezar con una palabra clave");
        }
        switch (t.getTexto().toUpperCase()) {
            case "CREATE": pos++; return crear();
            case "DROP": pos++; return borrar();
            case "SHOW": pos++; esperar("TABLES"); return new Comandos.MostrarTablas();
            case "DESCRIBE": case "DESC": pos++; return new Comandos.Describir(identificador("la tabla"));
            case "INSERT": pos++; return insertar();
            case "SELECT": pos++; return seleccionar();
            case "UPDATE": pos++; return actualizar();
            case "DELETE": pos++; return eliminar();
            case "LOAD": pos++; return cargar();
            case "SOURCE": pos++; return new Comandos.EjecutarScript(texto("la ruta del script"));
            case "TREE": pos++; return arbol();
            case "HELP": case "AYUDA": pos++; return new Comandos.Ayuda();
            case "EXIT": case "QUIT": case "SALIR": pos++; return new Comandos.Salir();
            default: throw error("Comando desconocido '" + t.getTexto() + "'");
        }
    }

    private Comando crear() {
        if (acepta("INDEX")) {
            esperar("ON");
            String tabla = identificador("la tabla");
            esperar("(");
            String campo = identificador("el campo");
            esperar(")");
            return new Comandos.CrearIndice(tabla, campo);
        }
        esperar("TABLE");
        String tabla = identificador("la tabla");
        esperar("(");
        List<Campo> campos = new ArrayList<>();
        do {
            String nombre = identificador("el campo");
            Tipo tipo = Tipo.desdeNombre(identificador("el tipo"));
            boolean pk = false;
            if (acepta("PRIMARY")) {
                esperar("KEY");
                pk = true;
            }
            campos.add(new Campo(nombre, tipo, pk));
        } while (acepta(","));
        esperar(")");
        return new Comandos.CrearTabla(tabla, campos);
    }

    private Comando borrar() {
        if (acepta("INDEX")) {
            esperar("ON");
            String tabla = identificador("la tabla");
            esperar("(");
            String campo = identificador("el campo");
            esperar(")");
            return new Comandos.BorrarIndice(tabla, campo);
        }
        esperar("TABLE");
        boolean siExiste = false;
        if (acepta("IF")) {
            esperar("EXISTS");
            siExiste = true;
        }
        return new Comandos.BorrarTabla(identificador("la tabla"), siExiste);
    }

    private Comando insertar() {
        esperar("INTO");
        String tabla = identificador("la tabla");
        List<String> columnas = null;
        if (acepta("(")) {
            columnas = new ArrayList<>();
            do {
                columnas.add(identificador("la columna"));
            } while (acepta(","));
            esperar(")");
        }
        esperar("VALUES");
        List<List<Object>> filas = new ArrayList<>();
        do {
            esperar("(");
            List<Object> valores = new ArrayList<>();
            do {
                valores.add(literal());
            } while (acepta(","));
            esperar(")");
            filas.add(valores);
        } while (acepta(","));
        return new Comandos.Insertar(tabla, columnas, filas);
    }

    private Comando seleccionar() {
        List<String> columnas = null;
        if (!acepta("*")) {
            columnas = new ArrayList<>();
            do {
                columnas.add(identificador("la columna"));
            } while (acepta(","));
        }
        esperar("FROM");
        String tabla = identificador("la tabla");
        return new Comandos.Seleccionar(tabla, columnas, dondeOpcional());
    }

    private Comando actualizar() {
        String tabla = identificador("la tabla");
        esperar("SET");
        Map<String, Object> cambios = new LinkedHashMap<>();
        do {
            String campo = identificador("el campo");
            esperar("=");
            if (cambios.put(campo, literal()) != null) {
                throw error("El campo '" + campo + "' aparece dos veces en SET");
            }
        } while (acepta(","));
        return new Comandos.Actualizar(tabla, cambios, dondeOpcional());
    }

    private Comando eliminar() {
        esperar("FROM");
        String tabla = identificador("la tabla");
        return new Comandos.Eliminar(tabla, dondeOpcional());
    }

    private Comando cargar() {
        String tabla = identificador("la tabla");
        acepta("FROM");
        return new Comandos.Cargar(tabla, texto("la ruta del CSV"));
    }

    private Comando arbol() {
        String tabla = identificador("la tabla");
        String campo = actual().getTipo() == Token.Tipo.PALABRA ? identificador("el campo") : null;
        return new Comandos.VerArbol(tabla, campo);
    }

    private Condicion dondeOpcional() {
        if (!acepta("WHERE")) {
            return null;
        }
        String campo = identificador("el campo");
        if (acepta("BETWEEN")) {
            Object desde = literal();
            esperar("AND");
            Object hasta = literal();
            return new Condicion(campo, Operador.ENTRE, desde, hasta);
        }
        Token t = actual();
        Operador op = t.getTipo() == Token.Tipo.SIMBOLO ? Operador.desdeSimbolo(t.getTexto()) : null;
        if (op == null || op == Operador.ENTRE) {
            throw error("Se esperaba un operador (=, !=, <, <=, >, >=, BETWEEN)");
        }
        pos++;
        return new Condicion(campo, op, literal());
    }

    private Object literal() {
        Token t = actual();
        switch (t.getTipo()) {
            case NUMERO:
            case TEXTO:
                pos++;
                return t.getValor();
            case PALABRA:
                if (t.es("TRUE") || t.es("VERDADERO")) {
                    pos++;
                    return Boolean.TRUE;
                }
                if (t.es("FALSE") || t.es("FALSO")) {
                    pos++;
                    return Boolean.FALSE;
                }
                if (t.es("NULL")) {
                    throw error("Este motor no admite NULL: todos los campos son obligatorios");
                }
                throw error("'" + t.getTexto() + "' no es un valor. Los textos van entre comillas");
            default:
                throw error("Se esperaba un valor");
        }
    }
}
