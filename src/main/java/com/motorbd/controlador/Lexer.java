package com.motorbd.controlador;

import com.motorbd.modelo.ErrorMotor;

import java.util.ArrayList;
import java.util.List;

/**
 * Analizador lexico: convierte el texto en una lista de tokens.
 *
 *   SELECT * FROM alumnos WHERE edad >= 18
 *   -> PALABRA(SELECT) SIMBOLO(*) PALABRA(FROM) PALABRA(alumnos) PALABRA(WHERE)
 *      PALABRA(edad) SIMBOLO(>=) NUMERO(18) FIN
 *
 * Recorre el texto una sola vez: O(L), L = longitud del comando.
 */
public final class Lexer {

    private Lexer() {
    }

    public static List<Token> tokenizar(String entrada) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = entrada.length();
        while (i < n) {
            char c = entrada.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (Character.isLetter(c) || c == '_') {
                int inicio = i;
                while (i < n && (Character.isLetterOrDigit(entrada.charAt(i)) || entrada.charAt(i) == '_')) {
                    i++;
                }
                String palabra = entrada.substring(inicio, i);
                tokens.add(new Token(Token.Tipo.PALABRA, palabra, palabra, inicio));
            } else if (Character.isDigit(c) || (c == '-' && i + 1 < n
                    && (Character.isDigit(entrada.charAt(i + 1)) || entrada.charAt(i + 1) == '.'))
                    || (c == '.' && i + 1 < n && Character.isDigit(entrada.charAt(i + 1)))) {
                int inicio = i;
                i++;
                boolean decimal = c == '.';
                while (i < n && (Character.isDigit(entrada.charAt(i)) || entrada.charAt(i) == '.')) {
                    if (entrada.charAt(i) == '.') {
                        if (decimal) {
                            throw new ErrorMotor("Numero mal escrito cerca de la posicion " + inicio + ".");
                        }
                        decimal = true;
                    }
                    i++;
                }
                String num = entrada.substring(inicio, i);
                Object valor;
                try {
                    if (decimal) {
                        valor = Double.parseDouble(num);
                    } else {
                        long l = Long.parseLong(num);
                        valor = (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) ? (Object) (int) l : (Object) l;
                    }
                } catch (NumberFormatException e) {
                    throw new ErrorMotor("Numero invalido: " + num);
                }
                tokens.add(new Token(Token.Tipo.NUMERO, num, valor, inicio));
            } else if (c == '"' || c == '\'') {
                int inicio = i;
                char comilla = c;
                i++;
                StringBuilder sb = new StringBuilder();
                boolean cerrada = false;
                while (i < n) {
                    char d = entrada.charAt(i);
                    if (d == comilla) {
                        if (i + 1 < n && entrada.charAt(i + 1) == comilla) {
                            sb.append(comilla); // comilla doble "" dentro del texto
                            i += 2;
                            continue;
                        }
                        cerrada = true;
                        i++;
                        break;
                    }
                    sb.append(d);
                    i++;
                }
                if (!cerrada) {
                    throw new ErrorMotor("Falta cerrar las comillas que abren en la posicion " + inicio + ".");
                }
                tokens.add(new Token(Token.Tipo.TEXTO, entrada.substring(inicio, i), sb.toString(), inicio));
            } else if ((c == '<' || c == '>' || c == '!') && i + 1 < n
                    && (entrada.charAt(i + 1) == '=' || (c == '<' && entrada.charAt(i + 1) == '>'))) {
                tokens.add(new Token(Token.Tipo.SIMBOLO, entrada.substring(i, i + 2), null, i));
                i += 2;
            } else if ("(),=<>*;".indexOf(c) >= 0) {
                tokens.add(new Token(Token.Tipo.SIMBOLO, String.valueOf(c), null, i));
                i++;
            } else {
                throw new ErrorMotor("Caracter no permitido '" + c + "' en la posicion " + i + ".");
            }
        }
        tokens.add(new Token(Token.Tipo.FIN, "", null, n));
        return tokens;
    }
}
