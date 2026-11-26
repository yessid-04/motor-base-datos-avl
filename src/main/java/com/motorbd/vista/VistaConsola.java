package com.motorbd.vista;

import com.motorbd.controlador.Controlador;
import com.motorbd.controlador.Resultado;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * VISTA de consola: el REPL (Read-Eval-Print Loop).
 * Lee una linea, se la pasa al controlador e imprime el resultado.
 */
public class VistaConsola {

    private final Controlador controlador;
    private final BufferedReader entrada;
    private final PrintStream salida;

    public VistaConsola(Controlador controlador, InputStream entrada, PrintStream salida) {
        this.controlador = controlador;
        this.entrada = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
        this.salida = salida;
    }

    public void iniciar() {
        salida.println("==============================================================");
        salida.println(" MotorBD - motor de base de datos relacional con arbol AVL");
        salida.println(" Datos en: " + controlador.getModelo().getCarpeta().toAbsolutePath());
        salida.println(" Escriba HELP para ver los comandos y EXIT para salir.");
        salida.println("==============================================================");
        while (true) {
            salida.print("motorbd> ");
            salida.flush();
            String linea;
            try {
                linea = entrada.readLine();
            } catch (IOException e) {
                salida.println("Error leyendo la entrada: " + e.getMessage());
                return;
            }
            if (linea == null) {
                salida.println();
                return; // fin de la entrada (Ctrl+D o archivo terminado)
            }
            if (linea.isBlank()) {
                continue;
            }
            Resultado r = controlador.ejecutar(linea);
            salida.println(FormatoTabla.formatear(r));
            if (r.debeSalir()) {
                return;
            }
        }
    }
}
