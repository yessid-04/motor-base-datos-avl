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
import java.awt.RenderingHints;

/**
 * Dibuja un arbol AVL. Cada nodo muestra su clave y su factor de balance.
 * Para arboles grandes dibuja solo los primeros niveles.
 */
public class PanelArbol extends JPanel {

    private static final int MAX_NIVELES = 6;
    private static final int RADIO = 16;
    private static final int ALTO_NIVEL = 70;

    private AVLArbol<Object, ?> arbol;
    private String titulo = "Seleccione una tabla";

    public PanelArbol() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(900, 480));
    }

    public void mostrar(AVLArbol<Object, ?> arbol, String titulo) {
        this.arbol = arbol;
        this.titulo = titulo;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.DARK_GRAY);
        g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
        String encabezado = titulo;
        if (arbol != null) {
            encabezado += "   |   nodos: " + arbol.tamanio() + "   altura: " + arbol.altura();
            if (arbol.altura() > MAX_NIVELES) {
                encabezado += "   (se muestran " + MAX_NIVELES + " niveles)";
            }
        }
        g2.drawString(encabezado, 12, 20);
        if (arbol == null || arbol.getRaiz() == null) {
            return;
        }
        g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
        dibujar(g2, arbol.getRaiz(), 0, getWidth(), 60, 1);
    }

    private void dibujar(Graphics2D g, AVLNodo<Object, ?> nodo, int izq, int der, int y, int nivel) {
        int x = (izq + der) / 2;
        int yHijo = y + ALTO_NIVEL;
        if (nivel < MAX_NIVELES) {
            g.setColor(Color.GRAY);
            g.setStroke(new BasicStroke(1.2f));
            if (nodo.getIzquierdo() != null) {
                g.drawLine(x, y, (izq + x) / 2, yHijo);
                dibujar(g, nodo.getIzquierdo(), izq, x, yHijo, nivel + 1);
            }
            if (nodo.getDerecho() != null) {
                g.drawLine(x, y, (x + der) / 2, yHijo);
                dibujar(g, nodo.getDerecho(), x, der, yHijo, nivel + 1);
            }
        } else if (nodo.getIzquierdo() != null || nodo.getDerecho() != null) {
            g.setColor(Color.GRAY);
            g.drawString("...", x - 6, y + RADIO + 14);
        }
        int fb = nodo.getFactorBalance();
        String clave = String.valueOf(nodo.getClave());
        if (clave.length() > 10) {
            clave = clave.substring(0, 9) + "..";
        }
        FontMetrics fm = g.getFontMetrics();
        int ancho = Math.max(2 * RADIO, fm.stringWidth(clave) + 12);
        g.setColor(fb == 0 ? new Color(0xE7F1FB) : new Color(0xF9EEDA));
        g.fillRoundRect(x - ancho / 2, y - RADIO, ancho, 2 * RADIO, 2 * RADIO, 2 * RADIO);
        g.setColor(new Color(0x2F5D8A));
        g.drawRoundRect(x - ancho / 2, y - RADIO, ancho, 2 * RADIO, 2 * RADIO, 2 * RADIO);
        g.setColor(Color.BLACK);
        g.drawString(clave, x - fm.stringWidth(clave) / 2, y + 4);
        g.setColor(Color.GRAY);
        String fbTxt = "fb=" + fb;
        g.drawString(fbTxt, x - fm.stringWidth(fbTxt) / 2, y - RADIO - 3);
    }
}
