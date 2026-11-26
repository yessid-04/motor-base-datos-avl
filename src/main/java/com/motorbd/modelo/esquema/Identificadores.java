package com.motorbd.modelo.esquema;

import com.motorbd.modelo.ErrorMotor;

import java.util.regex.Pattern;

/** Reglas para nombres de tablas y campos (tambien evita rutas peligrosas en disco). */
public final class Identificadores {

    private static final Pattern VALIDO = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,63}");

    private Identificadores() {
    }

    public static void validar(String nombre, String que) {
        if (nombre == null || !VALIDO.matcher(nombre).matches()) {
            throw new ErrorMotor("Nombre de " + que + " invalido: '" + nombre
                    + "'. Use letras, numeros y _ (sin empezar por numero).");
        }
    }
}
