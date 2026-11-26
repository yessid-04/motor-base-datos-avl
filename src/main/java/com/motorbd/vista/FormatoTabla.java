package com.motorbd.vista;

import com.motorbd.controlador.Resultado;

import java.util.List;

/** Convierte un Resultado en texto con forma de tabla para la consola. */
public final class FormatoTabla {

    private static final int MAX_FILAS = 200;

    private FormatoTabla() {
    }

    public static String formatear(Resultado r) {
        StringBuilder sb = new StringBuilder();
        if (r.tieneTabla()) {
            List<String> cols = r.getColumnas();
            List<List<Object>> filas = r.getFilas();
            int mostrar = Math.min(filas.size(), MAX_FILAS);
            int[] ancho = new int[cols.size()];
            for (int i = 0; i < cols.size(); i++) {
                ancho[i] = cols.get(i).length();
            }
            for (int f = 0; f < mostrar; f++) {
                for (int i = 0; i < cols.size(); i++) {
                    ancho[i] = Math.max(ancho[i], texto(filas.get(f).get(i)).length());
                }
            }
            String separador = separador(ancho);
            sb.append(separador).append(linea(cols.toArray(), ancho)).append(separador);
            for (int f = 0; f < mostrar; f++) {
                sb.append(linea(filas.get(f).toArray(), ancho));
            }
            if (!filas.isEmpty()) {
                sb.append(separador);
            }
            if (filas.size() > mostrar) {
                sb.append("... ").append(filas.size() - mostrar).append(" fila(s) mas no mostradas\n");
            }
            if (r.getPlan() != null) {
                sb.append("Plan: ").append(r.getPlan()).append('\n');
            }
        }
        sb.append(r.getMensaje());
        return sb.toString();
    }

    private static String texto(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String separador(int[] ancho) {
        StringBuilder sb = new StringBuilder("+");
        for (int a : ancho) {
            sb.append("-".repeat(a + 2)).append('+');
        }
        return sb.append('\n').toString();
    }

    private static String linea(Object[] valores, int[] ancho) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < ancho.length; i++) {
            String t = texto(valores[i]);
            sb.append(' ').append(t).append(" ".repeat(ancho[i] - t.length())).append(" |");
        }
        return sb.append('\n').toString();
    }
}
