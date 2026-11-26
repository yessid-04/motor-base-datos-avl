package com.motorbd;

import com.motorbd.controlador.Controlador;
import com.motorbd.controlador.Resultado;
import com.motorbd.modelo.BaseDatos;
import com.motorbd.modelo.ErrorMotor;
import com.motorbd.vista.FormatoTabla;
import com.motorbd.vista.VistaConsola;
import com.motorbd.vista.VistaGrafica;
import com.motorbd.vista.clasica.VistaGraficaClasica;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Punto de entrada. Arma el MVC: Modelo (BaseDatos) -> Controlador -> Vista.
 *
 * Uso:
 *   java -jar target/motorbd.jar                 consola (REPL)
 *   java -jar target/motorbd.jar --gui           interfaz grafica
 *   java -jar target/motorbd.jar --gui-clasica   interfaz grafica anterior (version 1)
 *   java -jar target/motorbd.jar --datos carpeta usar otra carpeta de datos (por defecto ./data)
 *   java -jar target/motorbd.jar --script x.sql  ejecutar un script y salir
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Path carpeta = Paths.get("data");
        boolean gui = false;
        boolean guiClasica = false;
        String script = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--gui": gui = true; break;
                case "--gui-clasica": guiClasica = true; break;
                case "--datos": carpeta = Paths.get(args[++i]); break;
                case "--script": script = args[++i]; break;
                default:
                    System.err.println("Argumento desconocido: " + args[i]);
                    System.exit(2);
            }
        }

        BaseDatos modelo;
        try {
            modelo = new BaseDatos(carpeta);
        } catch (ErrorMotor e) {
            System.err.println("No se pudo iniciar el motor: " + e.getMessage());
            System.exit(1);
            return;
        }
        Controlador controlador = new Controlador(modelo);

        if (script != null) {
            Resultado r;
            try {
                r = controlador.ejecutarScript(Paths.get(script));
            } catch (ErrorMotor e) {
                r = Resultado.error("ERROR: " + e.getMessage());
            }
            System.out.println(FormatoTabla.formatear(r));
            System.exit(r.esExito() ? 0 : 1);
        }
        if (guiClasica) {
            VistaGraficaClasica.abrir(controlador);
        } else if (gui) {
            VistaGrafica.abrir(controlador);
        } else {
            new VistaConsola(controlador, System.in, System.out).iniciar();
        }
    }
}
