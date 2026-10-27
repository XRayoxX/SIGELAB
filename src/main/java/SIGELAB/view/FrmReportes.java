
package SIGELAB.view;

import SIGELAB.controller.ReporteController;
import SIGELAB.model.Laboratorio;
import SIGELAB.model.ResultadoReporte;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Dialogos;
import SIGELAB.util.ExportarPdfUtil;
import SIGELAB.util.ExportarUtil;
import SIGELAB.util.Fechas;
import SIGELAB.util.TemaSIGELAB;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FrmReportes {

    // =====================================================
    // COLORES OFICIALES
    // =====================================================

    private static final Color AZUL =
            new Color(54, 125, 170);

    private static final Color OSCURO_FONDO =
            new Color(13, 20, 32);

    private static final Color OSCURO_TARJETA =
            new Color(24, 35, 51);

    private static final Color OSCURO_CAMPO =
            new Color(17, 28, 43);

    private static final Color OSCURO_BORDE =
            new Color(42, 58, 76);

    private static final Color OSCURO_TEXTO =
            new Color(227, 234, 242);

    private static final Color OSCURO_SECUNDARIO =
            new Color(151, 167, 184);

    private static final Color CLARO_FONDO =
            new Color(244, 247, 250);

    private static final Color CLARO_TARJETA =
            Color.WHITE;

    private static final Color CLARO_CAMPO =
            new Color(236, 242, 247);

    private static final Color CLARO_BORDE =
            new Color(215, 224, 233);

    private static final Color CLARO_TEXTO =
            new Color(35, 51, 67);

    private static final Color CLARO_SECUNDARIO =
            new Color(100, 117, 135);

    // =====================================================
    // COMPONENTES VINCULADOS AL .FORM
    // =====================================================

    private JPanel panelPrincipal;
    private JScrollPane scrollContenido;
    private JPanel panelContenido;
    private JPanel panelEncabezado;
    private JPanel cardFiltros;
    private JPanel panelAcciones;
    private JPanel cardResultados;
    private JScrollPane scrollResultados;

    private JLabel lblTitulo;
    private JLabel lblSubtitulo;
    private JLabel lblSeccionFiltros;
    private JLabel lblSeccionResultados;
    private JLabel lblTotalRegistros;

    private JButton btnModoClaro;
    private JButton btnModoOscuro;

    private JComboBox cmbTipoReporte;
    private JComboBox cmbLaboratorio;
    private JComboBox cmbEstadoEquipo;

    private JTextField txtFechaDesde;
    private JTextField txtFechaHasta;
    private JTextField txtCodigoEquipo;

    private JButton btnGenerar;
    private JButton btnExportarPdf;
    private JButton btnExportarExcel;
    private JButton btnImprimir;
    private JButton btnLimpiar;
    private JButton btnRegresar;

    private JTable tblResultados;

    // =====================================================
    // CONTROLADOR
    // =====================================================

    private final JFrame ventana;

    private final ReporteController controller =
            new ReporteController();

    private boolean generando;
    private boolean reporteGenerado;

    private static final String[] TIPOS = {
            "Equipos por estado",
            "Préstamos activos",
            "Préstamos finalizados",
            "Préstamos vencidos",
            "Préstamos entre fechas",
            "Historial de un equipo",
            "Historial de mantenimiento",
            "Devoluciones entre fechas",
            "Equipos más prestados"
    };

    private static final String[] ESTADOS = {
            "TODOS",
            "DISPONIBLE",
            "PRESTADO",
            "DANADO",
            "MANTENIMIENTO",
            "BAJA"
    };

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FrmReportes(JFrame menuPrincipal) {

        ventana = new JFrame(
                "SIGELAB - Reportes y consultas"
        );

        ventana.setContentPane(panelPrincipal);

        ventana.setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );

        ventana.setMinimumSize(
                new Dimension(760, 520)
        );

        ventana.setExtendedState(
                JFrame.MAXIMIZED_BOTH
        );

        ventana.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {

                if (menuPrincipal != null) {
                    menuPrincipal.setVisible(true);
                }
            }
        });

        configurarScroll();
        configurarTabla();
        configurarCombos();
        configurarCalendarios();
        configurarEventos();
        aplicarTema();
        limpiarResultados();
        cargarLaboratorios();
    }

    public void mostrar() {

        aplicarTema();

        ventana.setExtendedState(
                JFrame.MAXIMIZED_BOTH
        );

        ventana.setVisible(true);
    }

    // =====================================================
    // COMBOS
    // =====================================================

    private void configurarCombos() {

        cmbTipoReporte.setModel(
                new DefaultComboBoxModel<>(TIPOS)
        );

        cmbEstadoEquipo.setModel(
                new DefaultComboBoxModel<>(ESTADOS)
        );

        DefaultComboBoxModel<Object> labs =
                new DefaultComboBoxModel<>();

        labs.addElement("TODOS");

        cmbLaboratorio.setModel(labs);

        cmbTipoReporte.setSelectedIndex(0);
        cmbEstadoEquipo.setSelectedIndex(0);

        // Todos los filtros disponibles.
        cmbLaboratorio.setEnabled(true);
        cmbEstadoEquipo.setEnabled(true);
        txtCodigoEquipo.setEnabled(true);
        txtFechaDesde.setEnabled(true);
        txtFechaHasta.setEnabled(true);

        cmbEstadoEquipo.setToolTipText(
                "Estado ACTUAL del equipo"
        );
    }

    private void cargarLaboratorios() {

        btnGenerar.setEnabled(false);

        new SwingWorker<List<Laboratorio>, Void>() {

            @Override
            protected List<Laboratorio> doInBackground() {

                return controller.laboratoriosActivos();
            }

            @Override
            protected void done() {

                try {

                    List<Laboratorio> lista = get();

                    DefaultComboBoxModel<Object> modelo =
                            new DefaultComboBoxModel<>();

                    modelo.addElement("TODOS");

                    for (Laboratorio lab : lista) {
                        modelo.addElement(lab);
                    }

                    cmbLaboratorio.setModel(modelo);

                    btnGenerar.setEnabled(true);

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    mostrarError(
                            "Carga de laboratorios interrumpida."
                    );

                } catch (ExecutionException e) {

                    mostrarError(
                            "Error al cargar laboratorios: "
                                    + mensajeError(e)
                    );
                }
            }

        }.execute();
    }

    // =====================================================
    // CALENDARIOS
    // =====================================================

    private void configurarCalendarios() {

        txtFechaDesde.setEditable(false);
        txtFechaHasta.setEditable(false);

        txtFechaDesde.setToolTipText(
                "Haga clic para seleccionar la fecha inicial"
        );

        txtFechaHasta.setToolTipText(
                "Haga clic para seleccionar la fecha final"
        );

        txtFechaDesde.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        txtFechaHasta.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        txtFechaDesde.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent e) {

                        elegirFecha(txtFechaDesde);
                    }
                }
        );

        txtFechaHasta.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent e) {

                        elegirFecha(txtFechaHasta);
                    }
                }
        );

        // Accesibilidad con teclado.
        instalarTeclaCalendario(txtFechaDesde);
        instalarTeclaCalendario(txtFechaHasta);
    }

    private void instalarTeclaCalendario(
            JTextField campo) {

        campo.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {

                if (e.getKeyCode() == KeyEvent.VK_ENTER
                        || e.getKeyCode() == KeyEvent.VK_SPACE) {

                    elegirFecha(campo);
                }
            }
        });
    }

    private void elegirFecha(JTextField campo) {

        if (generando) {
            return;
        }

        LocalDate inicial = Fechas.leerOpcional(
                campo.getText(),
                "fecha"
        );

        LocalDate seleccionada =
                FrmCalendario.seleccionarFecha(
                        ventana,
                        inicial
                );

        if (seleccionada != null) {

            campo.setText(
                    Fechas.formatear(seleccionada)
            );

            limpiarResultados();
        }
    }

    // =====================================================
    // EVENTOS
    // =====================================================

    private void configurarEventos() {

        btnGenerar.addActionListener(
                e -> generarReporte()
        );

        btnExportarPdf.addActionListener(
                e -> ejecutar(this::exportarPdf)
        );

        btnExportarExcel.addActionListener(
                e -> ejecutar(this::exportarExcel)
        );

        btnImprimir.addActionListener(
                e -> ejecutar(this::imprimir)
        );

        btnLimpiar.addActionListener(
                e -> limpiarFiltros()
        );

        btnRegresar.addActionListener(
                e -> ventana.dispose()
        );

        btnModoClaro.addActionListener(e -> {

            TemaSIGELAB.establecerModoOscuro(false);
            aplicarTema();
        });

        btnModoOscuro.addActionListener(e -> {

            TemaSIGELAB.establecerModoOscuro(true);
            aplicarTema();
        });

        cmbTipoReporte.addActionListener(
                e -> limpiarResultados()
        );

        cmbEstadoEquipo.addActionListener(
                e -> limpiarResultados()
        );

        cmbLaboratorio.addActionListener(
                e -> limpiarResultados()
        );

        txtCodigoEquipo.addActionListener(
                e -> generarReporte()
        );

        txtCodigoEquipo.getDocument()
                .addDocumentListener(
                        new javax.swing.event.DocumentListener() {

                            @Override
                            public void insertUpdate(
                                    javax.swing.event.DocumentEvent e) {
                                limpiarResultados();
                            }

                            @Override
                            public void removeUpdate(
                                    javax.swing.event.DocumentEvent e) {
                                limpiarResultados();
                            }

                            @Override
                            public void changedUpdate(
                                    javax.swing.event.DocumentEvent e) {
                                limpiarResultados();
                            }
                        }
                );
    }

    // =====================================================
    // GENERAR REPORTE
    // =====================================================

    private void generarReporte() {

        if (generando || !btnGenerar.isEnabled()) {
            return;
        }

        final String tipo =
                String.valueOf(
                        cmbTipoReporte.getSelectedItem()
                );

        final Integer laboratorio =
                obtenerLaboratorio();

        final String codigo =
                txtCodigoEquipo.getText();

        final String estado =
                String.valueOf(
                        cmbEstadoEquipo.getSelectedItem()
                );

        final String desde =
                txtFechaDesde.getText();

        final String hasta =
                txtFechaHasta.getText();

        generando = true;
        reporteGenerado = false;

        btnGenerar.setEnabled(false);
        btnGenerar.setText("Consultando...");

        btnExportarPdf.setEnabled(false);
        btnExportarExcel.setEnabled(false);
        btnImprimir.setEnabled(false);

        new SwingWorker<ResultadoReporte, Void>() {

            @Override
            protected ResultadoReporte doInBackground() {

                return controller.generar(
                        tipo,
                        laboratorio,
                        codigo,
                        estado,
                        desde,
                        hasta
                );
            }

            @Override
            protected void done() {

                try {

                    ResultadoReporte resultado = get();

                    mostrarResultado(resultado);

                    reporteGenerado = true;
                    actualizarBotones();

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    limpiarResultados();

                    mostrarError(
                            "Consulta interrumpida."
                    );

                } catch (ExecutionException e) {

                    limpiarResultados();

                    mostrarError(
                            mensajeError(e)
                    );

                } finally {

                    generando = false;

                    btnGenerar.setText(
                            "Generar reporte"
                    );

                    btnGenerar.setEnabled(true);
                }
            }

        }.execute();
    }

    private Integer obtenerLaboratorio() {

        Object seleccionado =
                cmbLaboratorio.getSelectedItem();

        if (seleccionado instanceof Laboratorio lab) {
            return lab.getIdLaboratorio();
        }

        return null;
    }

    private void mostrarResultado(
            ResultadoReporte resultado) {

        String[] columnas =
                resultado.getColumnas()
                        .toArray(new String[0]);

        DefaultTableModel modelo =
                new DefaultTableModel(columnas, 0) {

                    @Override
                    public boolean isCellEditable(
                            int fila, int columna) {

                        return false;
                    }
                };

        for (Object[] fila : resultado.getFilas()) {

            modelo.addRow(fila);
        }

        tblResultados.setModel(modelo);

        tblResultados.setAutoResizeMode(
                JTable.AUTO_RESIZE_OFF
        );

        for (int i = 0;
             i < tblResultados.getColumnCount();
             i++) {

            tblResultados.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(180);
        }

        lblTotalRegistros.setText(
                "Total de registros: "
                        + resultado.getFilas().size()
        );

        lblSeccionResultados.setText(
                "RESULTADOS | "
                        + cmbTipoReporte.getSelectedItem()
        );

        aplicarEstiloTabla();
    }

    private void limpiarResultados() {

        reporteGenerado = false;

        tblResultados.setModel(
                new DefaultTableModel()
        );

        lblTotalRegistros.setText(
                "Total de registros: 0"
        );

        lblSeccionResultados.setText(
                "RESULTADOS DE LA CONSULTA"
        );

        actualizarBotones();
    }

    private void actualizarBotones() {

        boolean hayDatos =
                reporteGenerado
                        && tblResultados.getRowCount() > 0;

        btnExportarPdf.setEnabled(hayDatos);
        btnExportarExcel.setEnabled(hayDatos);
        btnImprimir.setEnabled(hayDatos);
    }

    // =====================================================
    // EXPORTACIONES
    // =====================================================

    private void exportarPdf() {

        exigirResultados();

        File archivo = ExportarPdfUtil.exportar(
                ventana,
                tblResultados,
                String.valueOf(
                        cmbTipoReporte.getSelectedItem()
                )
        );

        if (archivo != null) {

            Dialogos.info(
                    ventana,
                    "PDF guardado en:\n"
                            + archivo.getAbsolutePath()
            );
        }
    }

    private void exportarExcel() {

        exigirResultados();

        File archivo = ExportarUtil.exportarCsv(
                ventana,
                tblResultados,
                "reporte-sigelab"
        );

        if (archivo != null) {

            Dialogos.info(
                    ventana,
                    "Archivo CSV guardado en:\n"
                            + archivo.getAbsolutePath()
            );
        }
    }

    private void imprimir() {

        exigirResultados();

        try {

            tblResultados.print(
                    JTable.PrintMode.FIT_WIDTH,
                    new MessageFormat(
                            "SIGELAB - "
                                    + cmbTipoReporte.getSelectedItem()
                    ),
                    new MessageFormat(
                            "Página {0}"
                    )
            );

        } catch (PrinterException e) {

            throw new BusinessException(
                    "Error de impresión: "
                            + e.getMessage()
            );
        }
    }

    private void exigirResultados() {

        if (!reporteGenerado
                || tblResultados.getRowCount() == 0) {

            throw new BusinessException(
                    "Genere primero un reporte con resultados."
            );
        }
    }

    // =====================================================
    // LIMPIAR
    // =====================================================

    private void limpiarFiltros() {

        if (generando) {
            return;
        }

        cmbTipoReporte.setSelectedIndex(0);
        cmbLaboratorio.setSelectedIndex(0);
        cmbEstadoEquipo.setSelectedIndex(0);

        txtCodigoEquipo.setText("");
        txtFechaDesde.setText("");
        txtFechaHasta.setText("");

        limpiarResultados();
    }

    // =====================================================
    // TABLA Y SCROLL
    // =====================================================

    private void configurarTabla() {

        tblResultados.setRowHeight(30);

        tblResultados.setFillsViewportHeight(true);

        tblResultados.setAutoResizeMode(
                JTable.AUTO_RESIZE_OFF
        );

        tblResultados.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tblResultados.getTableHeader()
                .setReorderingAllowed(false);
    }

    private void configurarScroll() {

        scrollContenido.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        scrollContenido.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );

        scrollContenido.getVerticalScrollBar()
                .setUnitIncrement(20);

        scrollContenido.getHorizontalScrollBar()
                .setUnitIncrement(20);

        scrollResultados.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        scrollResultados.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );

        scrollResultados.getHorizontalScrollBar()
                .setUnitIncrement(20);
    }

    // =====================================================
    // TEMA SIGELAB
    // =====================================================

    private void aplicarTema() {

        boolean oscuro =
                TemaSIGELAB.esOscuro();

        Color fondo = oscuro
                ? OSCURO_FONDO
                : CLARO_FONDO;

        Color tarjeta = oscuro
                ? OSCURO_TARJETA
                : CLARO_TARJETA;

        Color campo = oscuro
                ? OSCURO_CAMPO
                : CLARO_CAMPO;

        Color borde = oscuro
                ? OSCURO_BORDE
                : CLARO_BORDE;

        Color texto = oscuro
                ? OSCURO_TEXTO
                : CLARO_TEXTO;

        Color secundario = oscuro
                ? OSCURO_SECUNDARIO
                : CLARO_SECUNDARIO;

        panelPrincipal.setBackground(fondo);
        panelContenido.setBackground(fondo);
        panelEncabezado.setBackground(fondo);
        panelAcciones.setBackground(fondo);

        scrollContenido.getViewport()
                .setBackground(fondo);

        cardFiltros.setBackground(tarjeta);
        cardResultados.setBackground(tarjeta);

        cardFiltros.setBorder(
                new LineBorder(borde, 1, true)
        );

        cardResultados.setBorder(
                new LineBorder(borde, 1, true)
        );

        lblTitulo.setFont(
                new Font("Segoe UI", Font.BOLD, 28)
        );

        lblTitulo.setForeground(texto);

        lblSubtitulo.setForeground(secundario);

        lblSubtitulo.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );

        lblSeccionFiltros.setForeground(texto);
        lblSeccionResultados.setForeground(texto);

        lblSeccionFiltros.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );

        lblSeccionResultados.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );

        lblTotalRegistros.setForeground(secundario);

        aplicarEtiquetas(cardFiltros, secundario);

        JTextField[] campos = {
                txtFechaDesde,
                txtFechaHasta,
                txtCodigoEquipo
        };

        for (JTextField input : campos) {

            input.setBackground(campo);
            input.setForeground(texto);
            input.setCaretColor(texto);

            input.setFont(
                    new Font("Segoe UI", Font.PLAIN, 13)
            );

            input.setBorder(
                    new CompoundBorder(
                            new LineBorder(borde, 1, true),
                            new EmptyBorder(7, 10, 7, 10)
                    )
            );
        }

        JComboBox[] combos = {
                cmbTipoReporte,
                cmbLaboratorio,
                cmbEstadoEquipo
        };

        for (JComboBox combo : combos) {

            combo.setBackground(campo);
            combo.setForeground(texto);

            combo.setFont(
                    new Font("Segoe UI", Font.PLAIN, 12)
            );

            combo.setBorder(
                    new LineBorder(borde, 1, true)
            );
        }

        configurarBotonPrincipal(btnGenerar);

        JButton[] secundarios = {
                btnExportarPdf,
                btnExportarExcel,
                btnImprimir,
                btnLimpiar,
                btnRegresar
        };

        for (JButton boton : secundarios) {
            configurarBotonSecundario(
                    boton, campo, texto, borde
            );
        }

        configurarBotonTema(
                btnModoClaro,
                !oscuro,
                tarjeta,
                texto,
                borde
        );

        configurarBotonTema(
                btnModoOscuro,
                oscuro,
                tarjeta,
                texto,
                borde
        );

        scrollResultados.setBorder(
                new LineBorder(borde, 1, true)
        );

        aplicarEstiloTabla();

        panelPrincipal.revalidate();
        panelPrincipal.repaint();
    }

    private void aplicarEtiquetas(
            Container contenedor,
            Color color) {

        for (Component componente
                : contenedor.getComponents()) {

            if (componente instanceof JLabel etiqueta
                    && componente != lblSeccionFiltros) {

                etiqueta.setForeground(color);

                etiqueta.setFont(
                        new Font(
                                "Segoe UI",
                                Font.PLAIN,
                                12
                        )
                );
            }

            if (componente instanceof Container hijo) {

                aplicarEtiquetas(hijo, color);
            }
        }
    }

    private void aplicarEstiloTabla() {

        boolean oscuro = TemaSIGELAB.esOscuro();

        Color fondo = oscuro
                ? OSCURO_TARJETA
                : CLARO_TARJETA;

        Color texto = oscuro
                ? OSCURO_TEXTO
                : CLARO_TEXTO;

        Color encabezado = oscuro
                ? OSCURO_CAMPO
                : CLARO_CAMPO;

        Color borde = oscuro
                ? OSCURO_BORDE
                : CLARO_BORDE;

        tblResultados.setBackground(fondo);
        tblResultados.setForeground(texto);
        tblResultados.setGridColor(borde);

        tblResultados.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );

        tblResultados.setSelectionBackground(
                oscuro
                        ? new Color(32, 69, 96)
                        : new Color(216, 231, 241)
        );

        tblResultados.setSelectionForeground(texto);

        JTableHeader header =
                tblResultados.getTableHeader();

        header.setBackground(encabezado);
        header.setForeground(texto);

        header.setPreferredSize(
                new Dimension(
                        header.getWidth(),
                        36
                )
        );

        header.setDefaultRenderer(
                new DefaultTableCellRenderer() {

                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table,
                            Object value,
                            boolean selected,
                            boolean focus,
                            int row,
                            int column) {

                        JLabel label =
                                (JLabel) super
                                        .getTableCellRendererComponent(
                                                table,
                                                value,
                                                selected,
                                                focus,
                                                row,
                                                column
                                        );

                        label.setOpaque(true);
                        label.setBackground(encabezado);
                        label.setForeground(texto);

                        label.setFont(
                                new Font(
                                        "Segoe UI",
                                        Font.BOLD,
                                        11
                                )
                        );

                        label.setBorder(
                                new EmptyBorder(5, 10, 5, 10)
                        );

                        return label;
                    }
                }
        );
    }

    private void configurarBotonPrincipal(
            JButton boton) {

        boton.setBackground(AZUL);
        boton.setForeground(Color.WHITE);
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setFocusPainted(false);

        boton.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );

        boton.setBorder(
                new EmptyBorder(10, 16, 10, 16)
        );
    }

    private void configurarBotonSecundario(
            JButton boton,
            Color fondo,
            Color texto,
            Color borde) {

        boton.setBackground(fondo);
        boton.setForeground(texto);
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setFocusPainted(false);

        boton.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );

        boton.setBorder(
                new CompoundBorder(
                        new LineBorder(borde, 1, true),
                        new EmptyBorder(9, 15, 9, 15)
                )
        );
    }

    private void configurarBotonTema(
            JButton boton,
            boolean activo,
            Color fondo,
            Color texto,
            Color borde) {

        boton.setFont(
                new Font(
                        "Segoe UI Symbol",
                        Font.PLAIN,
                        20
                )
        );

        boton.setOpaque(true);
        boton.setFocusPainted(false);

        boton.setBackground(
                activo ? AZUL : fondo
        );

        boton.setForeground(
                activo ? Color.WHITE : texto
        );

        boton.setBorder(
                new LineBorder(
                        activo ? AZUL : borde,
                        1,
                        true
                )
        );
    }

    // =====================================================
    // ERRORES
    // =====================================================

    private void ejecutar(Runnable accion) {

        Dialogos.ejecutar(
                ventana,
                accion
        );
    }

    private void mostrarError(String mensaje) {

        JOptionPane.showMessageDialog(
                ventana,
                mensaje,
                "SIGELAB - Reportes",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private String mensajeError(
            ExecutionException e) {

        Throwable causa = e.getCause();

        if (causa == null) {
            return "Error inesperado.";
        }

        return causa.getMessage() == null
                ? causa.toString()
                : causa.getMessage();
    }
}
