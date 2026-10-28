package SIGELAB.util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Diseño oficial "Laboratory Command Center — versión formal" para los formularios de SIGELAB.
 *
 * El modo claro / oscuro lo decide TemaSIGELAB.esOscuro(); esta clase solo contiene la paleta
 * oficial y la forma de pintar cada componente. Uso en un formulario:
 *
 *   1) Marcar una vez los roles visuales (tarjeta, titulo, botonPrincipal, botonPeligro, ...).
 *   2) Llamar EstiloFormulario.aplicar(panelPrincipal) al abrir y cada vez que cambie el tema.
 *
 * Todo lo que no tenga un rol marcado recibe el estilo base (etiquetas secundarias,
 * campos, combos, tablas y botones secundarios).
 */
public final class EstiloFormulario {

    // Azul principal oficial (#367DAA), igual en ambos modos
    public static final Color AZUL = new Color(0x367DAA);

    private static final String ROL = "sigelab.rol";
    private static final String ESCUCHA = "sigelab.escucha";
    private static final String HOVER = "sigelab.hover";

    private static final String TARJETA = "tarjeta";
    private static final String TITULO = "titulo";
    private static final String SUBTITULO = "subtitulo";
    private static final String SECCION = "seccion";
    private static final String INDICACION = "indicacion";
    private static final String DESTACADO = "destacado";
    private static final String BOTON_PRINCIPAL = "principal";
    private static final String BOTON_PELIGRO = "peligro";
    private static final String BOTON_TEMA_CLARO = "tema-claro";
    private static final String BOTON_TEMA_OSCURO = "tema-oscuro";

    private EstiloFormulario() {
        // Clase de utilidades.
    }

    // ========================================
    // PALETA OFICIAL
    // ========================================

    private static boolean oscuro() {
        return TemaSIGELAB.esOscuro();
    }

    public static Color fondo() {
        return oscuro() ? new Color(0x0D1420) : new Color(0xF4F7FA);
    }

    public static Color navegacion() {
        return oscuro() ? new Color(0x111C2B) : new Color(0xECF2F7);
    }

    public static Color tarjeta() {
        return oscuro() ? new Color(0x182333) : Color.WHITE;
    }

    public static Color borde() {
        return oscuro() ? new Color(0x2A3A4C) : new Color(0xD7E0E9);
    }

    public static Color texto() {
        return oscuro() ? new Color(0xE3EAF2) : new Color(0x233343);
    }

    public static Color textoSecundario() {
        return oscuro() ? new Color(0x97A7B8) : new Color(0x647587);
    }

    public static Color seleccion() {
        return oscuro() ? new Color(0x204560) : new Color(0xD8E7F1);
    }

    // Fondo de un campo editable
    public static Color campo() {
        return oscuro() ? navegacion() : Color.WHITE;
    }

    // Fondo de un campo de solo lectura: se distingue del editable
    public static Color campoSoloLectura() {
        return oscuro() ? new Color(0x0F1724) : navegacion();
    }

    // Azul para textos destacados: en modo oscuro se aclara para que se lea
    public static Color azulTexto() {
        return oscuro() ? new Color(0x76B9E4) : AZUL;
    }

    public static Font fuente(int estilo, int tamano) {
        return new Font("Segoe UI", estilo, tamano);
    }

    // ========================================
    // ROLES VISUALES (se marcan una sola vez)
    // ========================================

    public static void tarjeta(JPanel... paneles) {
        marcar(TARJETA, paneles);
    }

    public static void titulo(JLabel etiqueta) {
        marcar(TITULO, etiqueta);
    }

    public static void subtitulo(JLabel etiqueta) {
        marcar(SUBTITULO, etiqueta);
    }

    public static void seccion(JLabel... etiquetas) {
        marcar(SECCION, etiquetas);
    }

    public static void indicacion(JLabel... etiquetas) {
        marcar(INDICACION, etiquetas);
    }

    public static void destacado(JLabel... etiquetas) {
        marcar(DESTACADO, etiquetas);
    }

    // Guardar, Registrar, Confirmar, Buscar...
    public static void botonPrincipal(JButton... botones) {
        marcar(BOTON_PRINCIPAL, botones);
    }

    // Desactivar, Eliminar, Anular...
    public static void botonPeligro(JButton... botones) {
        marcar(BOTON_PELIGRO, botones);
    }

    // Botones ☀ / ☾ del encabezado
    public static void botonesTema(JButton claro, JButton oscuro) {
        marcar(BOTON_TEMA_CLARO, claro);
        marcar(BOTON_TEMA_OSCURO, oscuro);
    }

    private static void marcar(String rol, JComponent... componentes) {
        for (JComponent c : componentes) {
            c.putClientProperty(ROL, rol);
        }
    }

    private static String rol(Component c) {
        return c instanceof JComponent j ? (String) j.getClientProperty(ROL) : null;
    }

    // ========================================
    // VENTANA Y DESPLAZAMIENTO
    // ========================================

    // Scroll general del formulario: aparece solo cuando el contenido no cabe en pantalla
    public static void configurarScroll(JScrollPane scroll) {
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getHorizontalScrollBar().setUnitIncrement(20);
    }

    /**
     * Pantalla completa (maximizada) ocupando el área útil de la pantalla, sin tapar la
     * barra de tareas de Windows. Conserva la barra de título con minimizar y cerrar.
     * Si el usuario restaura la ventana, conserva el tamaño de la pantalla en lugar de encogerse.
     */
    public static void pantallaCompleta(JFrame ventana) {
        ventana.setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        ventana.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    // ========================================
    // APLICAR TEMA
    // ========================================

    public static void aplicar(JPanel panelPrincipal, JPanel... tarjetas) {
        tarjeta(tarjetas);
        estilizar(panelPrincipal, fondo());
        panelPrincipal.revalidate();
        panelPrincipal.repaint();
    }

    private static void estilizar(Component c, Color fondoPadre) {
        if (c instanceof JScrollPane scroll) {
            estilizarScroll(scroll, fondoPadre);
            return;
        }
        if (c instanceof JPanel panel) {
            Color fondo = fondoPadre;
            if (TARJETA.equals(rol(panel))) {
                fondo = tarjeta();
                panel.setBorder(new LineBorder(borde(), 1, true));
            }
            panel.setOpaque(true);
            panel.setBackground(fondo);
            for (Component hijo : panel.getComponents()) {
                estilizar(hijo, fondo);
            }
            return;
        }
        if (c instanceof JLabel etiqueta) {
            estilizarEtiqueta(etiqueta);
        } else if (c instanceof JButton boton) {
            estilizarBoton(boton);
        } else if (c instanceof JTextField campo) {
            estilizarCampo(campo);
        } else if (c instanceof JComboBox<?> combo) {
            estilizarCombo(combo);
        } else if (c instanceof JSpinner spinner) {
            estilizarSpinner(spinner);
        } else if (c instanceof JCheckBox || c instanceof JRadioButton) {
            c.setForeground(texto());
            c.setFont(fuente(Font.PLAIN, 13));
            ((JComponent) c).setOpaque(false);
        }
    }

    // ========================================
    // ETIQUETAS
    // ========================================

    private static void estilizarEtiqueta(JLabel etiqueta) {
        String rol = rol(etiqueta);
        if (TITULO.equals(rol)) {
            etiqueta.setFont(fuente(Font.BOLD, 26));
            etiqueta.setForeground(texto());
        } else if (SUBTITULO.equals(rol)) {
            etiqueta.setFont(fuente(Font.PLAIN, 13));
            etiqueta.setForeground(textoSecundario());
        } else if (SECCION.equals(rol)) {
            etiqueta.setFont(fuente(Font.BOLD, 12));
            etiqueta.setForeground(texto());
        } else if (INDICACION.equals(rol)) {
            etiqueta.setFont(fuente(Font.PLAIN, 12));
            etiqueta.setForeground(textoSecundario());
        } else if (DESTACADO.equals(rol)) {
            etiqueta.setFont(fuente(Font.BOLD, 13));
            etiqueta.setForeground(azulTexto());
        } else {
            etiqueta.setFont(fuente(Font.PLAIN, 12));
            etiqueta.setForeground(textoSecundario());
        }
    }

    // ========================================
    // CAMPOS, COMBOS Y SPINNER
    // ========================================

    private static void estilizarCampo(JTextField campo) {
        campo.setFont(fuente(Font.PLAIN, 13));
        campo.setBorder(new CompoundBorder(new LineBorder(borde(), 1, true), new EmptyBorder(6, 10, 6, 10)));
        pintarTexto(campo);
        escucharEstado(campo);
    }

    // Editable / solo lectura se distinguen por el fondo y el color del texto
    private static void pintarTexto(JTextComponent campo) {
        boolean editable = campo.isEditable() && campo.isEnabled();
        campo.setBackground(editable ? campo() : campoSoloLectura());
        campo.setForeground(editable ? texto() : textoSecundario());
        campo.setDisabledTextColor(textoSecundario());
        campo.setCaretColor(texto());
        campo.setSelectionColor(seleccion());
        campo.setSelectedTextColor(texto());
    }

    private static void estilizarCombo(JComboBox<?> combo) {
        if (!(combo.getUI() instanceof ComboSigelab)) {
            combo.setUI(new ComboSigelab());
        }
        combo.setFont(fuente(Font.PLAIN, 13));
        combo.setBorder(new LineBorder(borde(), 1, true));
        pintarCombo(combo);
        escucharEstado(combo);
    }

    private static void pintarCombo(JComboBox<?> combo) {
        combo.setBackground(combo.isEnabled() ? campo() : campoSoloLectura());
        combo.setForeground(combo.isEnabled() ? texto() : textoSecundario());
        if (combo.getUI() instanceof ComboSigelab ui) {
            ui.colorearLista();
        }
        combo.repaint();
    }

    /**
     * ComboBox plano: el look and feel Metal pinta los combos deshabilitados en gris claro
     * aunque el tema sea oscuro. Esta versión toma siempre los colores de la paleta oficial.
     */
    private static final class ComboSigelab extends javax.swing.plaf.basic.BasicComboBoxUI {

        @Override
        protected JButton createArrowButton() {
            JButton flecha = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(comboBox.getBackground());
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(comboBox.isEnabled() ? textoSecundario() : borde());
                    g2.setStroke(new BasicStroke(1.6f));
                    int x = getWidth() / 2;
                    int y = getHeight() / 2;
                    g2.drawLine(x - 4, y - 2, x, y + 2);
                    g2.drawLine(x, y + 2, x + 4, y - 2);
                    g2.dispose();
                }
            };
            flecha.setBorder(new EmptyBorder(0, 0, 0, 0));
            flecha.setFocusable(false);
            flecha.setPreferredSize(new Dimension(28, 28));
            return flecha;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle limites, boolean foco) {
            g.setColor(comboBox.getBackground());
            g.fillRect(limites.x, limites.y, limites.width, limites.height);
        }

        @Override
        public void paintCurrentValue(Graphics g, Rectangle limites, boolean foco) {
            @SuppressWarnings("unchecked")
            ListCellRenderer<Object> renderer = (ListCellRenderer<Object>) comboBox.getRenderer();
            Component c = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false, false);
            c.setFont(comboBox.getFont());
            c.setForeground(comboBox.getForeground());
            c.setBackground(comboBox.getBackground());
            if (c instanceof JComponent j) {
                j.setOpaque(true);
                j.setBorder(new EmptyBorder(0, 8, 0, 4));
            }
            currentValuePane.paintComponent(g, c, comboBox, limites.x, limites.y, limites.width, limites.height, false);
        }

        // Lista desplegable con los mismos colores que el resto del formulario
        void colorearLista() {
            if (popup != null) {
                JList<?> lista = popup.getList();
                lista.setBackground(campo());
                lista.setForeground(texto());
                lista.setSelectionBackground(seleccion());
                lista.setSelectionForeground(texto());
                lista.setFont(fuente(Font.PLAIN, 13));
                if (popup instanceof JComponent menu) {
                    menu.setBorder(new LineBorder(borde(), 1));
                }
            }
        }
    }

    private static void estilizarSpinner(JSpinner spinner) {
        spinner.setFont(fuente(Font.PLAIN, 13));
        spinner.setBorder(new LineBorder(borde(), 1, true));
        spinner.setBackground(campo());
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            JFormattedTextField campo = editor.getTextField();
            campo.setFont(fuente(Font.PLAIN, 13));
            campo.setBorder(new EmptyBorder(6, 10, 6, 10));
            pintarTexto(campo);
            escucharEstado(campo);
        }
    }

    // Si el formulario cambia editable/enabled, el campo se vuelve a pintar solo
    private static void escucharEstado(JComponent c) {
        if (c.getClientProperty(ESCUCHA) != null) {
            return;
        }
        c.putClientProperty(ESCUCHA, Boolean.TRUE);
        c.addPropertyChangeListener(evento -> {
            String propiedad = evento.getPropertyName();
            if (!"editable".equals(propiedad) && !"enabled".equals(propiedad)) {
                return;
            }
            if (c instanceof JTextComponent t) {
                pintarTexto(t);
            } else if (c instanceof JComboBox<?> combo) {
                pintarCombo(combo);
            } else if (c instanceof JButton b) {
                pintarBoton(b);
            }
        });
    }

    // ========================================
    // BOTONES
    // ========================================

    private static void estilizarBoton(JButton boton) {
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setFocusPainted(false);
        boton.setRolloverEnabled(true);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pintarBoton(boton);
        if (boton.getClientProperty(ESCUCHA) == null) {
            escucharEstado(boton);
            boton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    boton.putClientProperty(HOVER, Boolean.TRUE);
                    pintarBoton(boton);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    boton.putClientProperty(HOVER, null);
                    pintarBoton(boton);
                }
            });
        }
    }

    private static void pintarBoton(JButton boton) {
        String rol = rol(boton);
        boolean activo = boton.isEnabled();
        boolean hover = activo && boton.getClientProperty(HOVER) != null;

        if (BOTON_TEMA_CLARO.equals(rol) || BOTON_TEMA_OSCURO.equals(rol)) {
            boolean seleccionado = BOTON_TEMA_OSCURO.equals(rol) == oscuro();
            boton.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
            boton.setBackground(seleccionado ? AZUL : (hover ? seleccion() : tarjeta()));
            boton.setForeground(seleccionado ? Color.WHITE : texto());
            boton.setBorder(new LineBorder(seleccionado ? AZUL : borde(), 1, true));
            return;
        }

        boton.setFont(fuente(Font.BOLD, 12));
        if (BOTON_PRINCIPAL.equals(rol)) {
            Color fondo = !activo ? (oscuro() ? new Color(0x24384B) : new Color(0xC9D9E5))
                    : hover ? new Color(0x2C6A91) : AZUL;
            boton.setBackground(fondo);
            boton.setForeground(activo ? Color.WHITE : textoSecundario());
            boton.setBorder(new CompoundBorder(new LineBorder(fondo, 1, true), new EmptyBorder(8, 18, 8, 18)));
        } else if (BOTON_PELIGRO.equals(rol)) {
            Color fondo = oscuro() ? (hover ? new Color(0x4C2830) : new Color(0x3E2229))
                    : (hover ? new Color(0xF6DCDC) : new Color(0xFBEBEB));
            Color linea = oscuro() ? new Color(0x7A3A44) : new Color(0xDAA8A8);
            boton.setBackground(activo ? fondo : tarjeta());
            boton.setForeground(!activo ? textoSecundario() : oscuro() ? new Color(0xEBA5A5) : new Color(0x9C3434));
            boton.setBorder(new CompoundBorder(new LineBorder(activo ? linea : borde(), 1, true), new EmptyBorder(8, 16, 8, 16)));
        } else {
            boton.setBackground(hover ? seleccion() : tarjeta());
            boton.setForeground(activo ? texto() : textoSecundario());
            boton.setBorder(new CompoundBorder(new LineBorder(borde(), 1, true), new EmptyBorder(8, 16, 8, 16)));
        }
    }

    // ========================================
    // SCROLL, TABLAS Y ÁREAS DE TEXTO
    // ========================================

    private static void estilizarScroll(JScrollPane scroll, Color fondoPadre) {
        Component vista = scroll.getViewport().getView();
        instalarBarras(scroll);

        if (vista instanceof JTable tabla) {
            estilizarTabla(tabla);
            scroll.setBorder(new LineBorder(borde(), 1, true));
            scroll.setBackground(tarjeta());
            scroll.getViewport().setBackground(tarjeta());
        } else if (vista instanceof JTextArea area) {
            area.setFont(fuente(Font.PLAIN, 13));
            area.setBorder(new EmptyBorder(6, 10, 6, 10));
            pintarTexto(area);
            escucharEstado(area);
            scroll.setBorder(new LineBorder(borde(), 1, true));
            scroll.getViewport().setBackground(area.getBackground());
        } else {
            // Scroll general del formulario: transparente sobre el fondo
            scroll.setBorder(null);
            scroll.setBackground(fondoPadre);
            scroll.getViewport().setBackground(fondoPadre);
            if (vista != null) {
                estilizar(vista, fondoPadre);
            }
        }
    }

    private static void estilizarTabla(JTable tabla) {
        tabla.setFont(fuente(Font.PLAIN, 12));
        tabla.setRowHeight(30);
        tabla.setBackground(tarjeta());
        tabla.setForeground(texto());
        tabla.setGridColor(borde());
        tabla.setShowHorizontalLines(true);
        tabla.setShowVerticalLines(false);
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setSelectionBackground(seleccion());
        tabla.setSelectionForeground(texto());
        tabla.setFillsViewportHeight(true);
        if (!(tabla.getDefaultRenderer(Object.class) instanceof CeldaSigelab)) {
            tabla.setDefaultRenderer(Object.class, new CeldaSigelab());
        }

        JTableHeader encabezado = tabla.getTableHeader();
        if (encabezado != null) {
            encabezado.setReorderingAllowed(false);
            encabezado.setBackground(navegacion());
            encabezado.setForeground(textoSecundario());
            if (!(encabezado.getDefaultRenderer() instanceof EncabezadoSigelab)) {
                encabezado.setDefaultRenderer(new EncabezadoSigelab());
            }
        }
    }

    // Celdas con margen interno y filas alternadas muy suaves; los colores se leen al pintar
    private static final class CeldaSigelab extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, seleccionada, false, fila, columna);
            setFont(fuente(Font.PLAIN, 12));
            setBorder(new EmptyBorder(0, 10, 0, 10));
            if (seleccionada) {
                setBackground(seleccion());
                setForeground(texto());
            } else {
                Color alterna = oscuro() ? new Color(0x1B2738) : new Color(0xF8FAFC);
                setBackground(fila % 2 == 0 ? tarjeta() : alterna);
                setForeground(texto());
            }
            return this;
        }
    }

    private static final class EncabezadoSigelab extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, false, false, fila, columna);
            setOpaque(true);
            setBackground(navegacion());
            setForeground(textoSecundario());
            setFont(fuente(Font.BOLD, 11));
            setHorizontalAlignment(LEADING);
            Border linea = new MatteBorder(0, 0, 1, columna < tabla.getColumnCount() - 1 ? 1 : 0, borde());
            setBorder(new CompoundBorder(linea, new EmptyBorder(8, 10, 8, 10)));
            return this;
        }
    }

    // Barras de desplazamiento discretas en ambos modos
    private static void instalarBarras(JScrollPane scroll) {
        for (JScrollBar barra : new JScrollBar[]{scroll.getVerticalScrollBar(), scroll.getHorizontalScrollBar()}) {
            if (!(barra.getUI() instanceof BarraSigelab)) {
                barra.setUI(new BarraSigelab());
            }
            barra.setOpaque(false);
        }
    }

    private static final class BarraSigelab extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            // Los colores se leen al pintar, según el tema actual
        }

        @Override
        protected JButton createDecreaseButton(int orientacion) {
            return botonVacio();
        }

        @Override
        protected JButton createIncreaseButton(int orientacion) {
            return botonVacio();
        }

        private JButton botonVacio() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            // Pista transparente
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isThumbRollover() ? textoSecundario() : borde());
            g2.fillRoundRect(r.x + 3, r.y + 3, r.width - 6, r.height - 6, 8, 8);
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            return scrollbar.getOrientation() == JScrollBar.VERTICAL ? new Dimension(12, 48) : new Dimension(48, 12);
        }
    }
}
