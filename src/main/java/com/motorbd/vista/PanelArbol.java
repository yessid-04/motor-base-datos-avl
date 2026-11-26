package com.motorbd.vista;

import com.motorbd.modelo.indice.AVLArbol;
import com.motorbd.modelo.indice.AVLNodo;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Dibujo interactivo del arbol AVL.
 *
 * - Rueda del mouse: zoom. Arrastrar: mover. Doble clic: volver a encuadrar.
 * - Color del nodo segun su factor de balance (0 verde, +1/-1 ambar).
 * - buscar(clave) resalta el camino que recorre una busqueda desde la raiz,
 *   para ver que cuesta tantas comparaciones como la altura del arbol.
 */
public class PanelArbol extends JPanel {

    private static final int ALTO_NIVEL = 78;
    private static final int RADIO = 17;
    private static final int MAX_NIVELES = 8;

    private AVLArbol<Object, ?> arbol;
    private String titulo = "";
    private final Set<AVLNodo<?, ?>> camino = new HashSet<>();
    private AVLNodo<?, ?> encontrado;

    private double zoom = 1.0;
    private double desplX;
    private double desplY;
    private Point inicioArrastre;

    public PanelArbol() {
        setBackground(Tema.PAPEL);
        setPreferredSize(new Dimension(900, 480));
        MouseAdapter raton = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                inicioArrastre = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (inicioArrastre != null) {
                    desplX += e.getX() - inicioArrastre.x;
                    desplY += e.getY() - inicioArrastre.y;
                    inicioArrastre = e.getPoint();
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    encuadrar();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double factor = e.getWheelRotation() < 0 ? 1.15 : 1 / 1.15;
                double nuevo = Math.max(0.3, Math.min(4.0, zoom * factor));
                double f = nuevo / zoom;
                desplX = e.getX() - (e.getX() - desplX) * f;
                desplY = e.getY() - (e.getY() - desplY) * f;
                zoom = nuevo;
                repaint();
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);
        addMouseWheelListener(raton);
    }

    public void mostrar(AVLArbol<Object, ?> arbol, String titulo) {
        this.arbol = arbol;
        this.titulo = titulo;
        camino.clear();
        encontrado = null;
        encuadrar();
    }

    public void encuadrar() {
        zoom = 1.0;
        desplX = 0;
        desplY = 0;
        repaint();
    }

    /**
     * Resalta el camino de busqueda de la clave. Devuelve cuantas comparaciones
     * hizo (positivo si la encontro, negativo si no existe).
     */
    public int buscar(Object clave, Comparator<Object> comparador) {
        camino.clear();
        encontrado = null;
        if (arbol == null) {
            return 0;
        }
        int comparaciones = 0;
        AVLNodo<Object, ?> actual = arbol.getRaiz();
        while (actual != null) {
            camino.add(actual);
            comparaciones++;
            int c = comparador.compare(clave, actual.getClave());
            if (c == 0) {
                encontrado = actual;
                break;
            }
            actual = c < 0 ? actual.getIzquierdo() : actual.getDerecho();
        }
        repaint();
        return encontrado != null ? comparaciones : -comparaciones;
    }

    public void limpiarBusqueda() {
        camino.clear();
        encontrado = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (arbol == null || arbol.getRaiz() == null) {
            g2.setFont(Tema.NORMAL);
            g2.setColor(Tema.TEXTO_SUAVE);
            String msg = arbol == null ? "Seleccione una tabla para ver su arbol AVL" : "La tabla esta vacia";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
            g2.dispose();
            return;
        }

        AffineTransform original = g2.getTransform();
        g2.translate(desplX, desplY);
        g2.scale(zoom, zoom);
        g2.setFont(Tema.PEQUENA.deriveFont(Font.BOLD, 11.5f));
        dibujar(g2, arbol.getRaiz(), 10, getWidth() - 10, 50, 1);
        g2.setTransform(original);

        dibujarLeyenda(g2);
        g2.dispose();
    }

    private void dibujar(Graphics2D g, AVLNodo<Object, ?> nodo, int izq, int der, int y, int nivel) {
        int x = (izq + der) / 2;
        int yHijo = y + ALTO_NIVEL;
        if (nivel < MAX_NIVELES) {
            AVLNodo<Object, ?>[] hijos = hijos(nodo);
            int[][] rangos = {{izq, x}, {x, der}};
            for (int i = 0; i < 2; i++) {
                if (hijos[i] == null) {
                    continue;
                }
                boolean enCamino = camino.contains(nodo) && camino.contains(hijos[i]);
                g.setColor(enCamino ? Tema.AMBAR : new Color(0xC9C3B6));
                g.setStroke(new BasicStroke(enCamino ? 3f : 1.4f));
                g.drawLine(x, y, (rangos[i][0] + rangos[i][1]) / 2, yHijo);
                dibujar(g, hijos[i], rangos[i][0], rangos[i][1], yHijo, nivel + 1);
            }
        } else if (nodo.getIzquierdo() != null || nodo.getDerecho() != null) {
            g.setColor(Tema.TEXTO_SUAVE);
            g.drawString("...", x - 6, y + RADIO + 14);
        }

        int fb = nodo.getFactorBalance();
        String clave = String.valueOf(nodo.getClave());
        if (clave.length() > 12) {
            clave = clave.substring(0, 11) + "..";
        }
        FontMetrics fm = g.getFontMetrics();
        int ancho = Math.max(2 * RADIO, fm.stringWidth(clave) + 16);

        Color fondo = fb == 0 ? Tema.VERDE_SUAVE : Tema.AMBAR_SUAVE;
        Color borde = fb == 0 ? Tema.VERDE : Tema.AMBAR;
        Color letra = Tema.TEXTO;
        if (nodo == encontrado) {
            fondo = Tema.VERDE;
            borde = Tema.TINTA;
            letra = Color.WHITE;
        } else if (camino.contains(nodo)) {
            fondo = Tema.AMBAR;
            borde = Tema.TINTA;
        }
        // sombra
        g.setColor(new Color(0, 0, 0, 25));
        g.fillRoundRect(x - ancho / 2 + 2, y - RADIO + 3, ancho, 2 * RADIO, 2 * RADIO, 2 * RADIO);
        g.setColor(fondo);
        g.fillRoundRect(x - ancho / 2, y - RADIO, ancho, 2 * RADIO, 2 * RADIO, 2 * RADIO);
        g.setColor(borde);
        g.setStroke(new BasicStroke(camino.contains(nodo) ? 2.5f : 1.5f));
        g.drawRoundRect(x - ancho / 2, y - RADIO, ancho, 2 * RADIO, 2 * RADIO, 2 * RADIO);
        g.setColor(letra);
        g.drawString(clave, x - fm.stringWidth(clave) / 2, y + 4);

        String info = "h" + nodo.getAltura() + "  fb " + (fb > 0 ? "+" : "") + fb;
        g.setFont(Tema.PEQUENA);
        FontMetrics fmp = g.getFontMetrics();
        g.setColor(Tema.TEXTO_SUAVE);
        g.drawString(info, x - fmp.stringWidth(info) / 2, y - RADIO - 4);
        g.setFont(Tema.PEQUENA.deriveFont(Font.BOLD, 11.5f));
    }

    @SuppressWarnings("unchecked")
    private static AVLNodo<Object, ?>[] hijos(AVLNodo<Object, ?> n) {
        return new AVLNodo[] {n.getIzquierdo(), n.getDerecho()};
    }

    private void dibujarLeyenda(Graphics2D g) {
        int x = 14;
        int y = getHeight() - 16;
        g.setFont(Tema.PEQUENA);
        x = item(g, x, y, Tema.VERDE_SUAVE, Tema.VERDE, "balanceado (fb 0)");
        x = item(g, x, y, Tema.AMBAR_SUAVE, Tema.AMBAR, "inclinado (fb +1 / -1)");
        x = item(g, x, y, Tema.AMBAR, Tema.TINTA, "camino de busqueda");
        x = item(g, x, y, Tema.VERDE, Tema.TINTA, "encontrado");
        g.setColor(Tema.TEXTO_SUAVE);
        String ayuda = "rueda: zoom  |  arrastrar: mover  |  doble clic: encuadrar";
        g.drawString(ayuda, Math.max(x + 10, getWidth() - g.getFontMetrics().stringWidth(ayuda) - 14), y + 4);
        if (titulo != null && !titulo.isEmpty()) {
            g.setFont(Tema.NEGRILLA);
            g.setColor(Tema.TINTA);
            g.drawString(titulo, 14, 22);
        }
    }

    private int item(Graphics2D g, int x, int y, Color fondo, Color borde, String texto) {
        g.setColor(fondo);
        g.fillRoundRect(x, y - 6, 22, 12, 12, 12);
        g.setColor(borde);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x, y - 6, 22, 12, 12, 12);
        g.setColor(Tema.TEXTO_SUAVE);
        g.drawString(texto, x + 28, y + 4);
        return x + 34 + g.getFontMetrics().stringWidth(texto) + 14;
    }
}
