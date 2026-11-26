package com.motorbd.controlador;

import java.util.Collections;
import java.util.List;

/**
 * Respuesta del controlador a la vista. La vista decide como mostrarla
 * (tabla de texto en consola, JTable en la GUI).
 */
public final class Resultado {

    private final boolean exito;
    private final String mensaje;
    private final List<String> columnas;
    private final List<List<Object>> filas;
    private final String plan;
    private final boolean salir;
    private final String tablaAfectada;

    private Resultado(boolean exito, String mensaje, List<String> columnas, List<List<Object>> filas,
                      String plan, boolean salir, String tablaAfectada) {
        this.exito = exito;
        this.mensaje = mensaje;
        this.columnas = columnas;
        this.filas = filas;
        this.plan = plan;
        this.salir = salir;
        this.tablaAfectada = tablaAfectada;
    }

    public static Resultado ok(String mensaje) {
        return new Resultado(true, mensaje, Collections.emptyList(), Collections.emptyList(), null, false, null);
    }

    public static Resultado ok(String mensaje, String tablaAfectada) {
        return new Resultado(true, mensaje, Collections.emptyList(), Collections.emptyList(), null, false, tablaAfectada);
    }

    public static Resultado consulta(String mensaje, List<String> columnas, List<List<Object>> filas,
                                     String plan, String tabla) {
        return new Resultado(true, mensaje, columnas, filas, plan, false, tabla);
    }

    public static Resultado error(String mensaje) {
        return new Resultado(false, mensaje, Collections.emptyList(), Collections.emptyList(), null, false, null);
    }

    public static Resultado despedida() {
        return new Resultado(true, "Hasta luego.", Collections.emptyList(), Collections.emptyList(), null, true, null);
    }

    public boolean esExito() {
        return exito;
    }

    public String getMensaje() {
        return mensaje;
    }

    public List<String> getColumnas() {
        return columnas;
    }

    public List<List<Object>> getFilas() {
        return filas;
    }

    public String getPlan() {
        return plan;
    }

    public boolean debeSalir() {
        return salir;
    }

    public boolean tieneTabla() {
        return !columnas.isEmpty();
    }

    /** Tabla que el comando leyo o modifico (la GUI la usa para refrescar el arbol). */
    public String getTablaAfectada() {
        return tablaAfectada;
    }
}
