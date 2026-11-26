package com.motorbd.modelo.persistencia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector y escritor JSON minimo, sin librerias externas.
 *
 * Tipos Java usados: Map (objeto), List (arreglo), String, Long o Double (numero),
 * Boolean y null. Suficiente para el catalogo y las filas del motor.
 */
public final class Json {

    private Json() {
    }

    // ------------------------------------------------------------------
    // Escritura
    // ------------------------------------------------------------------

    public static String escribir(Object valor) {
        StringBuilder sb = new StringBuilder();
        escribir(valor, sb, 0);
        sb.append('\n');
        return sb.toString();
    }

    private static void escribir(Object v, StringBuilder sb, int nivel) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            escribirTexto((String) v, sb);
        } else if (v instanceof Double) {
            double d = (Double) v;
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("JSON no admite NaN ni infinito");
            }
            sb.append(d);
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else if (v instanceof Map) {
            Map<?, ?> mapa = (Map<?, ?>) v;
            if (mapa.isEmpty()) {
                sb.append("{}");
                return;
            }
            boolean enLinea = nivel >= 2;
            sb.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> e : mapa.entrySet()) {
                if (!primero) {
                    sb.append(',');
                }
                primero = false;
                if (enLinea) {
                    sb.append(' ');
                } else {
                    sb.append('\n').append("  ".repeat(nivel + 1));
                }
                escribirTexto(String.valueOf(e.getKey()), sb);
                sb.append(": ");
                escribir(e.getValue(), sb, nivel + 1);
            }
            if (enLinea) {
                sb.append(" }");
            } else {
                sb.append('\n').append("  ".repeat(nivel)).append('}');
            }
        } else if (v instanceof List) {
            List<?> lista = (List<?>) v;
            if (lista.isEmpty()) {
                sb.append("[]");
                return;
            }
            sb.append('[');
            boolean primero = true;
            for (Object o : lista) {
                if (!primero) {
                    sb.append(',');
                }
                primero = false;
                sb.append('\n').append("  ".repeat(nivel + 1));
                escribir(o, sb, nivel + 1);
            }
            sb.append('\n').append("  ".repeat(nivel)).append(']');
        } else {
            throw new IllegalArgumentException("Tipo no soportado en JSON: " + v.getClass());
        }
    }

    private static void escribirTexto(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------------
    // Lectura (analizador descendente recursivo)
    // ------------------------------------------------------------------

    public static Object leer(String texto) {
        Lector l = new Lector(texto);
        l.saltarEspacios();
        Object v = l.valor();
        l.saltarEspacios();
        if (l.pos != texto.length()) {
            throw l.error("Contenido extra despues del JSON");
        }
        return v;
    }

    /** Error de formato JSON (archivo danado). */
    public static class ErrorJson extends RuntimeException {
        public ErrorJson(String mensaje) {
            super(mensaje);
        }
    }

    private static final class Lector {
        private final String s;
        private int pos;

        Lector(String s) {
            this.s = s;
        }

        ErrorJson error(String msg) {
            return new ErrorJson(msg + " (posicion " + pos + ")");
        }

        void saltarEspacios() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        char actual() {
            if (pos >= s.length()) {
                throw error("Fin inesperado");
            }
            return s.charAt(pos);
        }

        void esperar(char c) {
            if (actual() != c) {
                throw error("Se esperaba '" + c + "'");
            }
            pos++;
        }

        Object valor() {
            char c = actual();
            switch (c) {
                case '{': return objeto();
                case '[': return arreglo();
                case '"': return texto();
                case 't': palabra("true"); return Boolean.TRUE;
                case 'f': palabra("false"); return Boolean.FALSE;
                case 'n': palabra("null"); return null;
                default:
                    if (c == '-' || Character.isDigit(c)) {
                        return numero();
                    }
                    throw error("Caracter inesperado '" + c + "'");
            }
        }

        void palabra(String p) {
            if (!s.startsWith(p, pos)) {
                throw error("Se esperaba " + p);
            }
            pos += p.length();
        }

        Map<String, Object> objeto() {
            esperar('{');
            Map<String, Object> mapa = new LinkedHashMap<>();
            saltarEspacios();
            if (actual() == '}') {
                pos++;
                return mapa;
            }
            while (true) {
                saltarEspacios();
                String clave = texto();
                saltarEspacios();
                esperar(':');
                saltarEspacios();
                mapa.put(clave, valor());
                saltarEspacios();
                if (actual() == ',') {
                    pos++;
                } else {
                    esperar('}');
                    return mapa;
                }
            }
        }

        List<Object> arreglo() {
            esperar('[');
            List<Object> lista = new ArrayList<>();
            saltarEspacios();
            if (actual() == ']') {
                pos++;
                return lista;
            }
            while (true) {
                saltarEspacios();
                lista.add(valor());
                saltarEspacios();
                if (actual() == ',') {
                    pos++;
                } else {
                    esperar(']');
                    return lista;
                }
            }
        }

        String texto() {
            esperar('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = actual();
                pos++;
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    char e = actual();
                    pos++;
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u':
                            if (pos + 4 > s.length()) {
                                throw error("Escape \\u incompleto");
                            }
                            try {
                                sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            } catch (NumberFormatException ex) {
                                throw error("Escape \\u invalido");
                            }
                            pos += 4;
                            break;
                        default: throw error("Escape invalido \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        Object numero() {
            int inicio = pos;
            if (actual() == '-') {
                pos++;
            }
            boolean decimal = false;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (Character.isDigit(c)) {
                    pos++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    decimal = true;
                    pos++;
                } else {
                    break;
                }
            }
            String n = s.substring(inicio, pos);
            try {
                return decimal ? (Object) Double.parseDouble(n) : (Object) Long.parseLong(n);
            } catch (NumberFormatException e) {
                throw error("Numero invalido '" + n + "'");
            }
        }
    }
}
