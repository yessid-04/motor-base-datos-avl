package com.motorbd.herramientas;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Genera los datasets de prueba en la carpeta indicada (por defecto ./datasets):
 *
 *   pequeno.sql        CREATE TABLE + 20 INSERT + indices (tabla estudiantes)
 *   mediano.csv        N registros (por defecto 10.000) en orden aleatorio
 *   cargar_mediano.sql CREATE TABLE + indices + LOAD del CSV (tabla estudiantes_m)
 *
 * Usa una semilla fija, asi que siempre genera los mismos datos.
 *
 * Uso: java -cp target/classes com.motorbd.herramientas.GeneradorDatasets [carpeta] [N]
 */
public final class GeneradorDatasets {

    private static final String[] NOMBRES = {"Ana", "Luis", "Camila", "Andres", "Valentina", "Santiago",
        "Sofia", "Mateo", "Isabella", "Juan", "Mariana", "Daniel", "Laura", "Felipe", "Paula", "Sebastian",
        "Gabriela", "Nicolas", "Daniela", "Julian", "Natalia", "David", "Carolina", "Esteban"};
    private static final String[] APELLIDOS = {"Gomez", "Rodriguez", "Martinez", "Lopez", "Garcia", "Perez",
        "Sanchez", "Ramirez", "Torres", "Diaz", "Vargas", "Rojas", "Moreno", "Castro", "Ortiz", "Rincon"};
    private static final String[] CARRERAS = {"Sistemas", "Industrial", "Electronica", "Civil",
        "Catastral", "Electrica", "Telematica", "Matematicas"};

    private GeneradorDatasets() {
    }

    public static void main(String[] args) throws IOException {
        Path carpeta = Paths.get(args.length > 0 ? args[0] : "datasets");
        int n = args.length > 1 ? Integer.parseInt(args[1]) : 10_000;
        Files.createDirectories(carpeta);
        Random azar = new Random(20261127L);

        generarPequeno(carpeta.resolve("pequeno.sql"), azar);
        generarMediano(carpeta.resolve("mediano.csv"), n, azar);
        generarScriptMediano(carpeta.resolve("cargar_mediano.sql"), carpeta.resolve("mediano.csv"));
        System.out.println("Datasets generados en " + carpeta.toAbsolutePath() + " (mediano: " + n + " filas)");
    }

    private static void generarPequeno(Path archivo, Random azar) throws IOException {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(archivo, StandardCharsets.UTF_8))) {
            out.println("-- Dataset pequeno: 20 estudiantes. Ejecutar con: SOURCE \"datasets/pequeno.sql\"");
            out.println("DROP TABLE IF EXISTS estudiantes");
            out.println("CREATE TABLE estudiantes (id INT PRIMARY KEY, nombre TEXT, carrera TEXT, "
                    + "semestre INT, promedio REAL, activo BOOL)");
            out.println("CREATE INDEX ON estudiantes (semestre)");
            out.println("CREATE INDEX ON estudiantes (carrera)");
            List<Integer> ids = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                ids.add(20261000 + i);
            }
            Collections.shuffle(ids, azar);
            for (int id : ids) {
                out.println(String.format(Locale.ROOT, "INSERT INTO estudiantes VALUES (%d, \"%s\", \"%s\", %d, %.1f, %s)",
                        id, nombre(azar), CARRERAS[azar.nextInt(CARRERAS.length)], 1 + azar.nextInt(10),
                        2.5 + azar.nextInt(26) / 10.0, azar.nextInt(10) < 8));
            }
        }
    }

    private static void generarMediano(Path archivo, int n, Random azar) throws IOException {
        List<Integer> ids = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            ids.add(i);
        }
        Collections.shuffle(ids, azar);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(archivo, StandardCharsets.UTF_8))) {
            out.println("id,nombre,carrera,semestre,promedio,activo");
            for (int id : ids) {
                out.println(String.format(Locale.ROOT, "%d,%s,%s,%d,%.2f,%s", id, nombre(azar),
                        CARRERAS[azar.nextInt(CARRERAS.length)], 1 + azar.nextInt(10),
                        2.0 + azar.nextInt(301) / 100.0, azar.nextInt(10) < 8));
            }
        }
    }

    private static void generarScriptMediano(Path archivo, Path csv) throws IOException {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(archivo, StandardCharsets.UTF_8))) {
            out.println("-- Dataset mediano. Ejecutar con: SOURCE \"datasets/cargar_mediano.sql\"");
            out.println("DROP TABLE IF EXISTS estudiantes_m");
            out.println("CREATE TABLE estudiantes_m (id INT PRIMARY KEY, nombre TEXT, carrera TEXT, "
                    + "semestre INT, promedio REAL, activo BOOL)");
            out.println("CREATE INDEX ON estudiantes_m (promedio)");
            out.println("CREATE INDEX ON estudiantes_m (carrera)");
            out.println("LOAD estudiantes_m \"" + csv.toString().replace('\\', '/') + "\"");
        }
    }

    private static String nombre(Random azar) {
        return NOMBRES[azar.nextInt(NOMBRES.length)] + " " + APELLIDOS[azar.nextInt(APELLIDOS.length)];
    }
}
