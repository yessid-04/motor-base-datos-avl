package com.motorbd.vista;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.List;

/**
 * Identidad visual de MotorBD: paleta, tipografias y componentes con estilo propio.
 * Todo se dibuja con Swing estandar, sin librerias externas.
 */
public final class Tema {

    // Paleta: azul tinta, ambar como acento, fondo papel
    public static final Color TINTA = new Color(0x14213D);
    public static final Color TINTA_CLARA = new Color(0x1F3157);
    public static final Color TINTA_SUAVE = new Color(0x2C416B);
    public static final Color AMBAR = new Color(0xF4A21A);
    public static final Color AMBAR_SUAVE = new Color(0xFDEFD3);
    public static final Color PAPEL = new Color(0xF6F4EF);
    public static final Color BLANCO = Color.WHITE;
    public static final Color LINEA = new Color(0xE2DED5);
    public static final Color TEXTO = new Color(0x1E2430);
    public static final Color TEXTO_SUAVE = new Color(0x6B7280);
    public static final Color VERDE = new Color(0x2A9D8F);
    public static final Color VERDE_SUAVE = new Color(0xDDF1EE);
    public static final Color AZUL = new Color(0x3D6FB6);
    public static final Color AZUL_SUAVE = new Color(0xE1EAF7);
    public static final Color CORAL = new Color(0xE76F51);
    public static final Color CORAL_SUAVE = new Color(0xFBE4DD);

    public static final Font TITULO;
    public static final Font NORMAL;
    public static final Font NEGRILLA;
    public static final Font PEQUENA;
    public static final Font MONO;

    static {
        String sans = elegir("Segoe UI", "Inter", "Helvetica Neue", "Arial", Font.SANS_SERIF);
        String mono = elegir("Cascadia Mono", "Consolas", "JetBrains Mono", "Menlo", Font.MONOSPACED);
        TITULO = new Font(sans, Font.BOLD, 20);
        NORMAL = new Font(sans, Font.PLAIN, 13);
        NEGRILLA = new Font(sans, Font.BOLD, 13);
        PEQUENA = new Font(sans, Font.PLAIN, 11);
        MONO = new Font(mono, Font.PLAIN, 14);
    }

    private Tema() {
    }

    private static String elegir(String... opciones) {
        List<String> disponibles;
        try {
            disponibles = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        } catch (Exception e) {
            return opciones[opciones.length - 1];
        }
        for (String o : opciones) {
            if (disponibles.contains(o)) {
                return o;
            }
        }
        return opciones[opciones.length - 1];
    }

    public static Border margen(int arriba, int izq, int abajo, int der) {
        return BorderFactory.createEmptyBorder(arriba, izq, abajo, der);
    }

    public static JLabel etiqueta(String texto, Font fuente, Color color) {
        JLabel l = new JLabel(texto);
        l.setFont(fuente);
        l.setForeground(color);
        return l;
    }

    /** Boton plano redondeado con color de fondo propio. */
    public static JButton boton(String texto, Color fondo, Color letra) {
        JButton b = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = fondo;
                if (getModel().isPressed()) {
                    c = c.darker();
                } else if (getModel().isRollover()) {
                    c = mezclar(c, Color.WHITE, 0.15);
                }
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(NEGRILLA);
        b.setForeground(letra);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setMargin(new Insets(7, 14, 7, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Pastilla de texto (chip) con fondo suave. */
    public static JLabel chip(String texto, Color fondo, Color letra) {
        JLabel l = new JLabel(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(PEQUENA.deriveFont(Font.BOLD));
        l.setBackground(fondo);
        l.setForeground(letra);
        l.setBorder(margen(4, 10, 4, 10));
        return l;
    }

    public static Color mezclar(Color a, Color b, double t) {
        return new Color(
                (int) (a.getRed() * (1 - t) + b.getRed() * t),
                (int) (a.getGreen() * (1 - t) + b.getGreen() * t),
                (int) (a.getBlue() * (1 - t) + b.getBlue() * t));
    }

    /** Panel con fondo blanco, borde fino y esquinas redondeadas. */
    public static void tarjeta(JComponent c) {
        c.setBackground(BLANCO);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINEA, 1, true), margen(12, 14, 12, 14)));
    }
}
