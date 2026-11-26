package com.motorbd.modelo;

/**
 * Error esperado del motor (tabla inexistente, tipo incorrecto, clave repetida...).
 * El mensaje esta pensado para mostrarse tal cual al usuario.
 */
public class ErrorMotor extends RuntimeException {

    public ErrorMotor(String mensaje) {
        super(mensaje);
    }

    public ErrorMotor(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
