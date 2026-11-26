package com.motorbd.vista.clasica;

import com.motorbd.controlador.Controlador;
import com.motorbd.controlador.Resultado;
import com.motorbd.modelo.ErrorMotor;
import com.motorbd.modelo.datos.Tabla;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * VISTA grafica (Swing), extra del enunciado.
 * Igual que la consola, solo envia texto al controlador y muestra el Resultado.
 */
public class VistaGraficaClasica extends JFrame {

    private final Controlador controlador;
    private final JTextField campoComando = new JTextField();
    private final JTextArea areaMensajes = new JTextArea(6, 60);
    private final DefaultTableModel modeloResultados = new DefaultTableModel();
    private final DefaultListModel<String> modeloTablas = new DefaultListModel<>();
    private final JList<String> listaTablas = new JList<>(modeloTablas);
    private final JComboBox<String> comboIndice = new JComboBox<>();
    private final PanelArbolClasico panelArbol = new PanelArbolClasico();
    private final JTabbedPane pestanas = new JTabbedPane();

    public VistaGraficaClasica(Controlador controlador) {
        super("MotorBD (interfaz clasica)");
        this.controlador = controlador;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        construir();
        refrescarTablas();
        pack();
        setLocationRelativeTo(null);
    }

    private void construir() {
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 13);

        // Barra de comando
        JPanel barra = new JPanel(new BorderLayout(6, 6));
        barra.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        campoComando.setFont(mono);
        JButton ejecutar = new JButton("Ejecutar");
        barra.add(new JLabel("Comando:"), BorderLayout.WEST);
        barra.add(campoComando, BorderLayout.CENTER);
        barra.add(ejecutar, BorderLayout.EAST);
        ejecutar.addActionListener(e -> ejecutarComando());
        campoComando.addActionListener(e -> ejecutarComando());

        // Lista de tablas
        listaTablas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaTablas.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                tablaSeleccionada();
            }
        });
        JPanel izquierda = new JPanel(new BorderLayout());
        izquierda.setBorder(BorderFactory.createTitledBorder("Tablas"));
        izquierda.add(new JScrollPane(listaTablas), BorderLayout.CENTER);
        izquierda.setPreferredSize(new Dimension(180, 400));

        // Resultados y arbol
        JTable tablaResultados = new JTable(modeloResultados);
        tablaResultados.setAutoCreateRowSorter(true);
        pestanas.addTab("Resultados", new JScrollPane(tablaResultados));

        JPanel panelArbolCompleto = new JPanel(new BorderLayout());
        JPanel selector = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selector.add(new JLabel("Indice:"));
        selector.add(comboIndice);
        comboIndice.addActionListener(e -> dibujarArbol());
        panelArbolCompleto.add(selector, BorderLayout.NORTH);
        panelArbolCompleto.add(new JScrollPane(panelArbol), BorderLayout.CENTER);
        pestanas.addTab("Arbol AVL", panelArbolCompleto);

        areaMensajes.setEditable(false);
        areaMensajes.setFont(mono);
        areaMensajes.setText(Controlador.AYUDA);

        JSplitPane centro = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pestanas, new JScrollPane(areaMensajes));
        centro.setResizeWeight(0.7);
        JSplitPane principal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, centro);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(barra, BorderLayout.NORTH);
        getContentPane().add(principal, BorderLayout.CENTER);
        setPreferredSize(new Dimension(1150, 720));
    }

    private void ejecutarComando() {
        String texto = campoComando.getText();
        if (texto.isBlank()) {
            return;
        }
        Resultado r = controlador.ejecutar(texto);
        if (r.debeSalir()) {
            dispose();
            System.exit(0);
        }
        areaMensajes.setText("> " + texto + "\n" + (r.getPlan() != null ? "Plan: " + r.getPlan() + "\n" : "")
                + r.getMensaje());
        if (r.tieneTabla()) {
            modeloResultados.setDataVector(
                    r.getFilas().stream().map(f -> f.toArray()).toArray(Object[][]::new),
                    r.getColumnas().toArray());
            pestanas.setSelectedIndex(0);
        }
        if (r.esExito()) {
            campoComando.setText("");
        }
        refrescarTablas();
        if (r.getTablaAfectada() != null) {
            listaTablas.setSelectedValue(nombreReal(r.getTablaAfectada()), true);
            dibujarArbol();
        }
    }

    private String nombreReal(String nombre) {
        for (int i = 0; i < modeloTablas.size(); i++) {
            if (modeloTablas.get(i).equalsIgnoreCase(nombre)) {
                return modeloTablas.get(i);
            }
        }
        return nombre;
    }

    private void refrescarTablas() {
        String seleccion = listaTablas.getSelectedValue();
        modeloTablas.clear();
        for (Tabla t : controlador.getModelo().listarTablas()) {
            modeloTablas.addElement(t.getNombre());
        }
        if (seleccion != null && modeloTablas.contains(seleccion)) {
            listaTablas.setSelectedValue(seleccion, false);
        }
    }

    private void tablaSeleccionada() {
        String tabla = listaTablas.getSelectedValue();
        comboIndice.removeAllItems();
        if (tabla == null) {
            panelArbol.mostrar(null, "Seleccione una tabla");
            return;
        }
        Tabla t = controlador.getModelo().tabla(tabla);
        comboIndice.addItem(t.getEsquema().getPrimario().getNombre() + " (primario)");
        for (String campo : t.camposIndexados()) {
            comboIndice.addItem(campo);
        }
        dibujarArbol();
    }

    private void dibujarArbol() {
        String tabla = listaTablas.getSelectedValue();
        Object item = comboIndice.getSelectedItem();
        if (tabla == null || item == null) {
            return;
        }
        String campo = item.toString().endsWith("(primario)") ? null : item.toString();
        try {
            panelArbol.mostrar(controlador.getModelo().arbol(tabla, campo),
                    tabla + " - indice " + (campo == null ? "primario" : campo));
        } catch (ErrorMotor e) {
            panelArbol.mostrar(null, e.getMessage());
        }
    }

    public static void abrir(Controlador controlador) {
        SwingUtilities.invokeLater(() -> new VistaGraficaClasica(controlador).setVisible(true));
    }
}
