package com.motorbd.modelo.esquema;

import com.motorbd.modelo.ErrorMotor;

/**
 * Tipos de dato soportados: entero, texto, real y booleano.
 * Cada tipo sabe convertir un literal al valor Java que lo representa:
 * INT -> Integer, REAL -> Double, TEXT -> String, BOOL -> Boolean.
 */
public enum Tipo {
    INT, TEXT, REAL, BOOL;

    /** Interpreta nombres de tipo, incluidos sinonimos en espanol. */
    public static Tipo desdeNombre(String nombre) {
        switch (nombre.toUpperCase()) {
            case "INT": case "INTEGER": case "ENTERO":
                return INT;
            case "TEXT": case "STRING": case "VARCHAR": case "TEXTO":
                return TEXT;
            case "REAL": case "DOUBLE": case "FLOAT": case "DECIMAL":
                return REAL;
            case "BOOL": case "BOOLEAN": case "BOOLEANO":
                return BOOL;
            default:
                throw new ErrorMotor("Tipo desconocido: " + nombre + ". Use INT, TEXT, REAL o BOOL.");
        }
    }

    /**
     * Convierte un valor ya interpretado (Integer, Long, Double, String o Boolean)
     * a este tipo. Lanza ErrorMotor si no es compatible.
     */
    public Object convertir(Object valor, String nombreCampo) {
        if (valor == null) {
            throw new ErrorMotor("El campo '" + nombreCampo + "' no admite valores nulos.");
        }
        switch (this) {
            case INT:
                if (valor instanceof Integer) {
                    return valor;
                }
                if (valor instanceof Long) {
                    long l = (Long) valor;
                    if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                        return (int) l;
                    }
                }
                break;
            case REAL:
                if (valor instanceof Number) {
                    return ((Number) valor).doubleValue();
                }
                break;
            case TEXT:
                if (valor instanceof String) {
                    return valor;
                }
                break;
            case BOOL:
                if (valor instanceof Boolean) {
                    return valor;
                }
                break;
            default:
                break;
        }
        throw new ErrorMotor("Valor " + describir(valor) + " no es valido para el campo '"
                + nombreCampo + "' de tipo " + this + ".");
    }

    /** Convierte un texto crudo (por ejemplo una celda de CSV) a este tipo. */
    public Object desdeTexto(String texto, String nombreCampo) {
        String t = texto.trim();
        try {
            switch (this) {
                case INT:
                    return Integer.parseInt(t);
                case REAL:
                    return Double.parseDouble(t);
                case BOOL:
                    if (t.equalsIgnoreCase("true") || t.equalsIgnoreCase("verdadero")) {
                        return true;
                    }
                    if (t.equalsIgnoreCase("false") || t.equalsIgnoreCase("falso")) {
                        return false;
                    }
                    break;
                case TEXT:
                    return texto;
                default:
                    break;
            }
        } catch (NumberFormatException e) {
            // se reporta abajo
        }
        throw new ErrorMotor("'" + texto + "' no es valido para el campo '" + nombreCampo
                + "' de tipo " + this + ".");
    }

    private static String describir(Object valor) {
        return valor instanceof String ? "\"" + valor + "\"" : String.valueOf(valor);
    }
}
