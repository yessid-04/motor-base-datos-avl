package com.motorbd.vista;

import com.motorbd.controlador.Controlador;
import com.motorbd.controlador.Resultado;
import com.motorbd.modelo.ErrorMotor;
import com.motorbd.modelo.datos.Tabla;
import com.motorbd.modelo.esquema.Campo;
import com.motorbd.modelo.indice.AVLArbol;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * VISTA grafica de MotorBD (Swing, sin librerias externas).
 *
 * Igual que la consola, solo envia texto al Controlador y muestra el Resultado.
 * Zonas: encabezado con resumen, barra lateral de tablas, editor de consultas con
 * atajos, y pestanas de Resultados, Arbol AVL (con busqueda paso a paso), Esquema e Historial.
 */
public class VistaGrafica extends JFrame {

    private final Controlador controlador;

    // Encabezado
    private final JLabel chipTablas = Tema.chip("", Tema.TINTA_SUAVE, Color.WHITE);
    private final JLabel chipRegistros = Tema.chip("", Tema.TINTA_SUAVE, Color.WHITE);

    // Barra lateral
    private final DefaultListModel<Tabla> modeloTablas = new DefaultListModel<>();
    private final JList<Tabla> listaTablas = new JList<>(modeloTablas);

    // Editor
    private final JTextArea editor = new JTextArea(4, 60);
    private final List<String> historial = new ArrayList<>();
    private int posHistorial;

    // Pestanas
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenedorPestanas = new JPanel(tarjetas);
    private final List<JButton> botonesPestana = new ArrayList<>();

    // Resultados
    private final DefaultTableModel modeloResultados = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int col) {
            return false;
        }
    };
    private final JLabel tituloResultados = Tema.etiqueta("Ejecute una consulta para ver resultados",
            Tema.NEGRILLA, Tema.TEXTO_SUAVE);

    // Arbol
    private final PanelArbol panelArbol = new PanelArbol();
    private final JComboBox<String> comboIndice = new JComboBox<>();
    private final JTextField campoBuscar = new JTextField(10);
    private final JLabel resultadoBusqueda = Tema.etiqueta(" ", Tema.NORMAL, Tema.TEXTO_SUAVE);

    // Esquema
    private final DefaultTableModel modeloEsquema = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int col) {
            return false;
        }
    };
    private final JLabel tituloEsquema = Tema.etiqueta("Seleccione una tabla", Tema.NEGRILLA, Tema.TINTA);

    // Historial
    private final JTextPane registro = new JTextPane();

    // Barra de estado
    private final JLabel insigniaPlan = Tema.chip("LISTO", Tema.VERDE_SUAVE, Tema.VERDE);
    private final JLabel mensajeEstado = Tema.etiqueta("Escriba un comando o use los atajos.", Tema.NORMAL, Tema.TEXTO);

    public VistaGrafica(Controlador controlador) {
        super("MotorBD");
        this.controlador = controlador;
        UIManager.put("ScrollBar.width", 10);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Tema.PAPEL);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(crearEncabezado(), BorderLayout.NORTH);
        getContentPane().add(crearBarraLateral(), BorderLayout.WEST);
        getContentPane().add(crearCentro(), BorderLayout.CENTER);
        getContentPane().add(crearBarraEstado(), BorderLayout.SOUTH);
        setPreferredSize(new Dimension(1280, 800));
        setMinimumSize(new Dimension(980, 620));
        pack();
        setLocationRelativeTo(null);
        refrescarTablas();
        if (!modeloTablas.isEmpty()) {
            listaTablas.setSelectedIndex(0);
        }
        mostrarPestana(0);
        anotar("Bienvenido. Datos en " + controlador.getModelo().getCarpeta().toAbsolutePath(), Tema.TEXTO_SUAVE);
        SwingUtilities.invokeLater(editor::requestFocusInWindow);
    }

    // ------------------------------------------------------------------
    // Construccion
    // ------------------------------------------------------------------

    private JComponent crearEncabezado() {
        JPanel enc = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, Tema.TINTA, getWidth(), 0, Tema.TINTA_CLARA));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Tema.AMBAR);
                g2.fillRect(0, getHeight() - 3, getWidth(), 3);
            }
        };
        enc.setBorder(Tema.margen(14, 20, 15, 20));

        JPanel marca = new JPanel();
        marca.setOpaque(false);
        marca.setLayout(new BoxLayout(marca, BoxLayout.X_AXIS));
        JComponent logo = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                // mini arbol: tres nodos y dos ramas
                g2.setColor(Tema.AMBAR);
                g2.setStroke(new java.awt.BasicStroke(2.2f));
                g2.drawLine(17, 10, 8, 27);
                g2.drawLine(17, 10, 26, 27);
                g2.fillOval(11, 4, 12, 12);
                g2.setColor(Color.WHITE);
                g2.fillOval(3, 22, 10, 10);
                g2.fillOval(21, 22, 10, 10);
            }
        };
        logo.setPreferredSize(new Dimension(36, 36));
        logo.setMaximumSize(new Dimension(36, 36));
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.add(Tema.etiqueta("MotorBD", Tema.TITULO, Color.WHITE));
        textos.add(Tema.etiqueta("Base de datos relacional con indices AVL", Tema.PEQUENA, new Color(0xB8C2D9)));
        marca.add(logo);
        marca.add(Box.createHorizontalStrut(12));
        marca.add(textos);
        enc.add(marca, BorderLayout.WEST);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        chips.setOpaque(false);
        chips.add(chipTablas);
        chips.add(chipRegistros);
        JButton ayuda = Tema.boton("Ayuda", Tema.TINTA_SUAVE, Color.WHITE);
        ayuda.addActionListener(e -> ejecutarYMostrar("HELP"));
        chips.add(ayuda);
        enc.add(chips, BorderLayout.EAST);
        return enc;
    }

    private JComponent crearBarraLateral() {
        JPanel lateral = new JPanel(new BorderLayout());
        lateral.setBackground(Tema.TINTA);
        lateral.setPreferredSize(new Dimension(250, 100));
        lateral.setBorder(Tema.margen(16, 12, 14, 12));

        JLabel titulo = Tema.etiqueta("TABLAS", Tema.PEQUENA.deriveFont(Font.BOLD), new Color(0x8E9BB8));
        titulo.setBorder(Tema.margen(0, 6, 10, 0));
        lateral.add(titulo, BorderLayout.NORTH);

        listaTablas.setBackground(Tema.TINTA);
        listaTablas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaTablas.setFixedCellHeight(76);
        listaTablas.setCellRenderer(new RenderTabla());
        listaTablas.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                tablaSeleccionada();
            }
        });
        listaTablas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && listaTablas.getSelectedValue() != null) {
                    ejecutarYMostrar("SELECT * FROM " + listaTablas.getSelectedValue().getNombre());
                }
            }
        });
        JScrollPane scroll = new JScrollPane(listaTablas,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Tema.TINTA);
        lateral.add(scroll, BorderLayout.CENTER);

        JPanel abajo = new JPanel(new GridLayout(0, 1, 0, 8));
        abajo.setOpaque(false);
        abajo.setBorder(Tema.margen(10, 0, 0, 0));
        JLabel pista = Tema.etiqueta("<html>Doble clic en una tabla<br>para ver todos sus registros</html>",
                Tema.PEQUENA, new Color(0x8E9BB8));
        pista.setBorder(Tema.margen(0, 6, 0, 0));
        JButton nueva = Tema.boton("+  Nueva tabla", Tema.AMBAR, Tema.TINTA);
        nueva.addActionListener(e -> ponerEnEditor(
                "CREATE TABLE nombre (id INT PRIMARY KEY, texto TEXT, numero REAL, activo BOOL)"));
        abajo.add(pista);
        abajo.add(nueva);
        lateral.add(abajo, BorderLayout.SOUTH);
        return lateral;
    }

    private JComponent crearCentro() {
        JPanel superior = new JPanel(new BorderLayout(0, 10));
        superior.setOpaque(false);
        superior.setBorder(Tema.margen(16, 18, 8, 18));
        superior.add(crearEditor(), BorderLayout.CENTER);

        JPanel inferior = new JPanel(new BorderLayout(0, 8));
        inferior.setOpaque(false);
        inferior.setBorder(Tema.margen(4, 18, 12, 18));
        inferior.add(crearBarraPestanas(), BorderLayout.NORTH);
        contenedorPestanas.setOpaque(false);
        contenedorPestanas.add(crearPestanaResultados(), "0");
        contenedorPestanas.add(crearPestanaArbol(), "1");
        contenedorPestanas.add(crearPestanaEsquema(), "2");
        contenedorPestanas.add(crearPestanaHistorial(), "3");
        inferior.add(contenedorPestanas, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, superior, inferior);
        split.setBorder(null);
        split.setDividerSize(6);
        split.setResizeWeight(0.0);
        split.setDividerLocation(230);
        split.setOpaque(false);
        split.setBackground(Tema.PAPEL);
        return split;
    }

    private JComponent crearEditor() {
        JPanel caja = new JPanel(new BorderLayout(0, 10));
        Tema.tarjeta(caja);

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        cabecera.add(Tema.etiqueta("Consulta", Tema.NEGRILLA, Tema.TINTA), BorderLayout.WEST);
        cabecera.add(Tema.etiqueta("Ctrl+Enter ejecuta  |  Ctrl+Arriba/Abajo recorre el historial  |  una linea = un comando",
                Tema.PEQUENA, Tema.TEXTO_SUAVE), BorderLayout.EAST);
        caja.add(cabecera, BorderLayout.NORTH);

        editor.setFont(Tema.MONO);
        editor.setForeground(Tema.TEXTO);
        editor.setBackground(new Color(0xFBFAF7));
        editor.setCaretColor(Tema.AMBAR.darker());
        editor.setLineWrap(true);
        editor.setWrapStyleWord(true);
        editor.setBorder(Tema.margen(10, 12, 10, 12));
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "ejecutar");
        editor.getActionMap().put("ejecutar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ejecutarEditor();
            }
        });
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK), "anterior");
        editor.getActionMap().put("anterior", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moverHistorial(-1);
            }
        });
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), "siguiente");
        editor.getActionMap().put("siguiente", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moverHistorial(1);
            }
        });
        JScrollPane scroll = new JScrollPane(editor);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.LINEA));
        scroll.setPreferredSize(new Dimension(600, 96));
        scroll.setMinimumSize(new Dimension(300, 70));
        caja.add(scroll, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new BorderLayout());
        acciones.setOpaque(false);
        JPanel atajos = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        atajos.setOpaque(false);
        atajos.add(Tema.etiqueta("Atajos:", Tema.PEQUENA, Tema.TEXTO_SUAVE));
        String[][] plantillas = {
            {"Consultar", "select"}, {"Insertar", "insert"}, {"Actualizar", "update"},
            {"Eliminar", "delete"}, {"Crear indice", "index"}, {"Ver arbol", "tree"}};
        for (String[] p : plantillas) {
            JButton b = Tema.boton(p[0], Tema.AZUL_SUAVE, Tema.TINTA);
            b.setFont(Tema.PEQUENA.deriveFont(Font.BOLD));
            b.addActionListener(e -> plantilla(p[1]));
            atajos.add(b);
        }
        acciones.add(atajos, BorderLayout.WEST);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        derecha.setOpaque(false);
        JButton limpiar = Tema.boton("Limpiar", Tema.PAPEL, Tema.TEXTO_SUAVE);
        limpiar.addActionListener(e -> {
            editor.setText("");
            editor.requestFocusInWindow();
        });
        JButton ejecutar = Tema.boton("Ejecutar  >", Tema.AMBAR, Tema.TINTA);
        ejecutar.addActionListener(e -> ejecutarEditor());
        derecha.add(limpiar);
        derecha.add(ejecutar);
        acciones.add(derecha, BorderLayout.EAST);
        caja.add(acciones, BorderLayout.SOUTH);
        return caja;
    }

    private JComponent crearBarraPestanas() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        barra.setOpaque(false);
        String[] nombres = {"Resultados", "Arbol AVL", "Esquema", "Historial"};
        for (int i = 0; i < nombres.length; i++) {
            final int indice = i;
            JButton b = new JButton(nombres[i]) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean activa = Boolean.TRUE.equals(getClientProperty("activa"));
                    g2.setColor(activa ? Tema.TINTA : (getModel().isRollover() ? Tema.LINEA : Tema.PAPEL));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            b.setFont(Tema.NEGRILLA);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setBorder(Tema.margen(7, 16, 7, 16));
            b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            b.addActionListener(e -> mostrarPestana(indice));
            botonesPestana.add(b);
            barra.add(b);
        }
        return barra;
    }

    private JComponent crearPestanaResultados() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        Tema.tarjeta(panel);
        panel.add(tituloResultados, BorderLayout.NORTH);
        JTable tabla = new JTable(modeloResultados);
        estilizarTabla(tabla);
        tabla.setAutoCreateRowSorter(true);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.LINEA));
        scroll.getViewport().setBackground(Tema.BLANCO);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent crearPestanaArbol() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        Tema.tarjeta(panel);
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controles.setOpaque(false);
        controles.add(Tema.etiqueta("Indice:", Tema.NEGRILLA, Tema.TINTA));
        comboIndice.setFont(Tema.NORMAL);
        comboIndice.addActionListener(e -> dibujarArbol());
        controles.add(comboIndice);
        controles.add(Box.createHorizontalStrut(14));
        controles.add(Tema.etiqueta("Buscar clave:", Tema.NEGRILLA, Tema.TINTA));
        campoBuscar.setFont(Tema.MONO);
        campoBuscar.addActionListener(e -> buscarEnArbol());
        controles.add(campoBuscar);
        JButton recorrer = Tema.boton("Recorrer", Tema.AMBAR, Tema.TINTA);
        recorrer.addActionListener(e -> buscarEnArbol());
        controles.add(recorrer);
        JButton encuadrar = Tema.boton("Encuadrar", Tema.AZUL_SUAVE, Tema.TINTA);
        encuadrar.addActionListener(e -> {
            panelArbol.limpiarBusqueda();
            panelArbol.encuadrar();
            resultadoBusqueda.setText(" ");
        });
        controles.add(encuadrar);
        controles.add(resultadoBusqueda);
        panel.add(controles, BorderLayout.NORTH);
        panelArbol.setBorder(BorderFactory.createLineBorder(Tema.LINEA));
        panel.add(panelArbol, BorderLayout.CENTER);
        return panel;
    }

    private JComponent crearPestanaEsquema() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        Tema.tarjeta(panel);
        panel.add(tituloEsquema, BorderLayout.NORTH);
        JTable tabla = new JTable(modeloEsquema);
        estilizarTabla(tabla);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.LINEA));
        scroll.getViewport().setBackground(Tema.BLANCO);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent crearPestanaHistorial() {
        JPanel panel = new JPanel(new BorderLayout());
        Tema.tarjeta(panel);
        registro.setEditable(false);
        registro.setFont(Tema.MONO.deriveFont(13f));
        registro.setBackground(Tema.BLANCO);
        JScrollPane scroll = new JScrollPane(registro);
        scroll.setBorder(null);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent crearBarraEstado() {
        JPanel barra = new JPanel(new BorderLayout(10, 0));
        barra.setBackground(Tema.BLANCO);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.LINEA), Tema.margen(8, 18, 8, 18)));
        barra.add(insigniaPlan, BorderLayout.WEST);
        barra.add(mensajeEstado, BorderLayout.CENTER);
        return barra;
    }

    private void estilizarTabla(JTable tabla) {
        tabla.setFont(Tema.NORMAL);
        tabla.setRowHeight(28);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(Tema.LINEA);
        tabla.setSelectionBackground(Tema.AMBAR_SUAVE);
        tabla.setSelectionForeground(Tema.TEXTO);
        tabla.setFillsViewportHeight(true);
        JTableHeader cab = tabla.getTableHeader();
        cab.setReorderingAllowed(false);
        cab.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foco, f, c);
                l.setBackground(Tema.TINTA);
                l.setForeground(Color.WHITE);
                l.setFont(Tema.NEGRILLA);
                l.setBorder(Tema.margen(8, 10, 8, 10));
                return l;
            }
        });
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, false, f, c);
                l.setBorder(Tema.margen(0, 10, 0, 10));
                if (!sel) {
                    l.setBackground(f % 2 == 0 ? Tema.BLANCO : new Color(0xFAF8F4));
                }
                l.setHorizontalAlignment(v instanceof Number ? SwingConstants.RIGHT : SwingConstants.LEFT);
                if (v instanceof Boolean) {
                    l.setText((Boolean) v ? "si" : "no");
                    l.setForeground((Boolean) v ? Tema.VERDE : Tema.CORAL);
                } else {
                    l.setForeground(Tema.TEXTO);
                }
                return l;
            }
        });
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    private void mostrarPestana(int indice) {
        tarjetas.show(contenedorPestanas, String.valueOf(indice));
        for (int i = 0; i < botonesPestana.size(); i++) {
            JButton b = botonesPestana.get(i);
            b.putClientProperty("activa", i == indice);
            b.setForeground(i == indice ? Color.WHITE : Tema.TEXTO);
            b.repaint();
        }
    }

    private void ponerEnEditor(String texto) {
        editor.setText(texto);
        editor.requestFocusInWindow();
        editor.setCaretPosition(texto.length());
    }

    /** Llena el editor con un comando de ejemplo usando la tabla seleccionada. */
    private void plantilla(String tipo) {
        Tabla t = listaTablas.getSelectedValue();
        if (t == null) {
            ponerEnEditor("CREATE TABLE nombre (id INT PRIMARY KEY, texto TEXT, numero REAL, activo BOOL)");
            estado("CONSEJO", Tema.AZUL_SUAVE, Tema.AZUL, "Primero cree o seleccione una tabla.");
            return;
        }
        String n = t.getNombre();
        List<Campo> campos = t.getEsquema().getCampos();
        String pk = t.getEsquema().getPrimario().getNombre();
        Campo otro = campos.stream().filter(c -> !c.esPrimario()).findFirst().orElse(t.getEsquema().getPrimario());
        switch (tipo) {
            case "select":
                ponerEnEditor("SELECT * FROM " + n + " WHERE " + pk + " = " + ejemplo(t.getEsquema().getPrimario()));
                break;
            case "insert": {
                StringBuilder sb = new StringBuilder("INSERT INTO " + n + " VALUES (");
                for (int i = 0; i < campos.size(); i++) {
                    sb.append(i > 0 ? ", " : "").append(ejemplo(campos.get(i)));
                }
                ponerEnEditor(sb.append(")").toString());
                break;
            }
            case "update":
                ponerEnEditor("UPDATE " + n + " SET " + otro.getNombre() + " = " + ejemplo(otro)
                        + " WHERE " + pk + " = " + ejemplo(t.getEsquema().getPrimario()));
                break;
            case "delete":
                ponerEnEditor("DELETE FROM " + n + " WHERE " + pk + " = " + ejemplo(t.getEsquema().getPrimario()));
                break;
            case "index":
                ponerEnEditor("CREATE INDEX ON " + n + " (" + otro.getNombre() + ")");
                break;
            case "tree":
                ponerEnEditor("TREE " + n);
                break;
            default:
                break;
        }
    }

    private static String ejemplo(Campo c) {
        switch (c.getTipo()) {
            case INT: return "1";
            case REAL: return "1.0";
            case BOOL: return "true";
            default: return "\"texto\"";
        }
    }

    private void moverHistorial(int paso) {
        if (historial.isEmpty()) {
            return;
        }
        posHistorial = Math.max(0, Math.min(historial.size(), posHistorial + paso));
        editor.setText(posHistorial == historial.size() ? "" : historial.get(posHistorial));
    }

    /** Ejecuta cada linea no vacia del editor (o solo lo seleccionado). */
    private void ejecutarEditor() {
        String texto = editor.getSelectedText() != null ? editor.getSelectedText() : editor.getText();
        List<String> lineas = new ArrayList<>();
        for (String l : texto.split("\\R")) {
            if (!l.isBlank() && !l.strip().startsWith("--")) {
                lineas.add(l.strip());
            }
        }
        if (lineas.isEmpty()) {
            return;
        }
        boolean todoOk = true;
        for (String l : lineas) {
            todoOk &= ejecutarYMostrar(l);
        }
        if (todoOk && editor.getSelectedText() == null) {
            editor.setText("");
        }
    }

    private boolean ejecutarYMostrar(String comando) {
        historial.remove(comando);
        historial.add(comando);
        posHistorial = historial.size();

        Resultado r = controlador.ejecutar(comando);
        if (r.debeSalir()) {
            dispose();
            System.exit(0);
        }
        anotar("> " + comando, Tema.TINTA);
        anotar("  " + r.getMensaje().replace("\n", "\n  "), r.esExito() ? Tema.VERDE : Tema.CORAL);

        if (!r.esExito()) {
            estado("ERROR", Tema.CORAL_SUAVE, Tema.CORAL, r.getMensaje().replaceFirst("^ERROR: ", ""));
        } else if (r.getPlan() != null) {
            String p = r.getPlan();
            if (p.startsWith("Indice primario")) {
                estado("INDICE PRIMARIO", Tema.VERDE_SUAVE, Tema.VERDE, p + "   |   " + r.getMensaje());
            } else if (p.startsWith("Indice secundario")) {
                estado("INDICE SECUNDARIO", Tema.AZUL_SUAVE, Tema.AZUL, p + "   |   " + r.getMensaje());
            } else {
                estado("RECORRIDO COMPLETO", Tema.AMBAR_SUAVE, new Color(0xA86A00), p + "   |   " + r.getMensaje());
            }
        } else {
            estado("OK", Tema.VERDE_SUAVE, Tema.VERDE, primeraLinea(r.getMensaje()));
        }

        if (r.tieneTabla()) {
            modeloResultados.setDataVector(
                    r.getFilas().stream().map(List::toArray).toArray(Object[][]::new), r.getColumnas().toArray());
            tituloResultados.setText(r.getFilas().size() + " fila(s)" + (r.getTablaAfectada() != null
                    ? "  en  " + r.getTablaAfectada() : ""));
            tituloResultados.setForeground(Tema.TINTA);
            mostrarPestana(0);
        } else if (r.getMensaje().contains("\n")) {
            mostrarPestana(3); // ayuda, arbol en texto o script: se lee mejor en el historial
        }

        Tabla seleccion = listaTablas.getSelectedValue();
        refrescarTablas();
        String destino = r.getTablaAfectada() != null ? r.getTablaAfectada()
                : (seleccion != null ? seleccion.getNombre() : null);
        seleccionarTabla(destino);
        if (comando.toUpperCase().startsWith("TREE")) {
            mostrarPestana(1);
        }
        return r.esExito();
    }

    private static String primeraLinea(String s) {
        int i = s.indexOf('\n');
        return i < 0 ? s : s.substring(0, i);
    }

    private void estado(String insignia, Color fondo, Color letra, String mensaje) {
        insigniaPlan.setText(insignia);
        insigniaPlan.setBackground(fondo);
        insigniaPlan.setForeground(letra);
        mensajeEstado.setText(mensaje);
    }

    private void anotar(String texto, Color color) {
        StyledDocument doc = registro.getStyledDocument();
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, color);
        try {
            String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            doc.insertString(doc.getLength(), (texto.startsWith(">") ? hora + "  " : "          ") + texto + "\n", a);
            registro.setCaretPosition(doc.getLength());
        } catch (javax.swing.text.BadLocationException e) {
            // no deberia ocurrir
        }
    }

    private void refrescarTablas() {
        modeloTablas.clear();
        int registros = 0;
        for (Tabla t : controlador.getModelo().listarTablas()) {
            modeloTablas.addElement(t);
            registros += t.tamanio();
        }
        chipTablas.setText(modeloTablas.size() + " tabla(s)");
        chipRegistros.setText(String.format("%,d registro(s)", registros));
        listaTablas.repaint();
    }

    private void seleccionarTabla(String nombre) {
        if (nombre == null) {
            tablaSeleccionada();
            return;
        }
        for (int i = 0; i < modeloTablas.size(); i++) {
            if (modeloTablas.get(i).getNombre().equalsIgnoreCase(nombre)) {
                listaTablas.setSelectedIndex(i);
                tablaSeleccionada();
                return;
            }
        }
        tablaSeleccionada();
    }

    private void tablaSeleccionada() {
        Tabla t = listaTablas.getSelectedValue();
        Object indicePrevio = comboIndice.getSelectedItem();
        comboIndice.removeAllItems();
        if (t == null) {
            panelArbol.mostrar(null, "");
            modeloEsquema.setDataVector(new Object[0][], new Object[] {"campo", "tipo", "indice"});
            tituloEsquema.setText("Seleccione una tabla");
            return;
        }
        comboIndice.addItem(t.getEsquema().getPrimario().getNombre() + " (primario)");
        for (String campo : t.camposIndexados()) {
            comboIndice.addItem(campo);
        }
        if (indicePrevio != null) {
            comboIndice.setSelectedItem(indicePrevio);
        }
        dibujarArbol();

        List<Object[]> filas = new ArrayList<>();
        for (Campo c : t.getEsquema().getCampos()) {
            String indice = c.esPrimario() ? "clave primaria (AVL)" : (t.estaIndexado(c.getNombre()) ? "indice AVL" : "-");
            filas.add(new Object[] {c.getNombre(), c.getTipo().name(), indice});
        }
        modeloEsquema.setDataVector(filas.toArray(new Object[0][]), new Object[] {"campo", "tipo", "indice"});
        tituloEsquema.setText("Tabla " + t.getNombre() + "  -  " + t.tamanio() + " registro(s)");
    }

    private void dibujarArbol() {
        Tabla t = listaTablas.getSelectedValue();
        Object item = comboIndice.getSelectedItem();
        if (t == null || item == null) {
            return;
        }
        String campo = item.toString().endsWith("(primario)") ? null : item.toString();
        try {
            AVLArbol<Object, ?> arbol = t.arbol(campo);
            panelArbol.mostrar(arbol, t.getNombre() + "  |  indice " + (campo == null ? "primario" : campo)
                    + "  |  " + arbol.tamanio() + " nodos, altura " + arbol.altura());
            resultadoBusqueda.setText(" ");
        } catch (ErrorMotor e) {
            panelArbol.mostrar(null, e.getMessage());
        }
    }

    /** Recorre el arbol buscando la clave escrita y muestra cuantas comparaciones hizo. */
    private void buscarEnArbol() {
        Tabla t = listaTablas.getSelectedValue();
        Object item = comboIndice.getSelectedItem();
        String texto = campoBuscar.getText().trim();
        if (t == null || item == null || texto.isEmpty()) {
            return;
        }
        Campo campo = item.toString().endsWith("(primario)") ? t.getEsquema().getPrimario()
                : t.getEsquema().campo(item.toString());
        Object clave;
        try {
            String limpio = texto.length() > 1 && (texto.startsWith("\"") || texto.startsWith("'"))
                    ? texto.substring(1, texto.length() - 1) : texto;
            clave = campo.getTipo().desdeTexto(limpio, campo.getNombre());
        } catch (ErrorMotor e) {
            resultadoBusqueda.setText(e.getMessage());
            resultadoBusqueda.setForeground(Tema.CORAL);
            return;
        }
        int n = panelArbol.buscar(clave, Tabla.COMPARADOR);
        if (n > 0) {
            resultadoBusqueda.setText("Encontrado en " + n + " comparacion(es)  (altura del arbol: "
                    + t.arbol(campo.esPrimario() ? null : campo.getNombre()).altura() + ")");
            resultadoBusqueda.setForeground(Tema.VERDE);
        } else {
            resultadoBusqueda.setText("No existe. Se descarto con " + (-n) + " comparacion(es)");
            resultadoBusqueda.setForeground(Tema.CORAL);
        }
    }

    // ------------------------------------------------------------------
    // Tarjeta de tabla en la barra lateral
    // ------------------------------------------------------------------

    private static final class RenderTabla extends DefaultListCellRenderer {
        private boolean seleccionada;

        @Override
        public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                      boolean sel, boolean foco) {
            Tabla t = (Tabla) valor;
            seleccionada = sel;
            String indices = t.camposIndexados().isEmpty() ? "solo clave primaria"
                    : "indices: " + String.join(", ", t.camposIndexados());
            if (indices.length() > 30) {
                indices = indices.substring(0, 28) + "...";
            }
            String gris = sel ? "#5b4a1e" : "#8E9BB8";
            setText("<html><span style='font-size:11px'><b>" + t.getNombre() + "</b></span><br>"
                    + "<span style='color:" + gris + "'>" + String.format("%,d", t.tamanio()) + " registros</span><br>"
                    + "<span style='color:" + gris + "'>" + indices + "</span></html>");
            setFont(Tema.NORMAL);
            setForeground(sel ? Tema.TINTA : Color.WHITE);
            setBorder(Tema.margen(8, 14, 8, 10));
            setOpaque(false);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(seleccionada ? Tema.AMBAR : Tema.TINTA_CLARA);
            g2.fillRoundRect(0, 3, getWidth(), getHeight() - 6, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void abrir(Controlador controlador) {
        SwingUtilities.invokeLater(() -> new VistaGrafica(controlador).setVisible(true));
    }
}
