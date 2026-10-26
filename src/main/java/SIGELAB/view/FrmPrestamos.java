package SIGELAB.view;

import SIGELAB.controller.PrestamoController;
import SIGELAB.model.DetallePrestamo;
import SIGELAB.model.Equipo;
import SIGELAB.model.Prestamo;
import SIGELAB.model.Solicitante;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Dialogos;
import SIGELAB.util.Fechas;
import SIGELAB.util.Sesion;
import SIGELAB.util.TablaUtil;
import SIGELAB.util.TemaSIGELAB;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class FrmPrestamos {

    private static final int DIAS_PRESTAMO_SUGERIDOS = 3;

    // =====================================================
    // PALETA OFICIAL FORMAL — LABORATORY COMMAND CENTER
    // =====================================================
    private static final Color OSCURO_FONDO = new Color(13, 20, 32);
    private static final Color OSCURO_NAVEGACION = new Color(17, 28, 43);
    private static final Color OSCURO_TARJETA = new Color(24, 35, 51);
    private static final Color OSCURO_BORDE = new Color(42, 58, 76);
    private static final Color OSCURO_TEXTO = new Color(227, 234, 242);
    private static final Color OSCURO_SECUNDARIO = new Color(151, 167, 184);
    private static final Color OSCURO_SELECCION = new Color(32, 69, 96);

    private static final Color CLARO_FONDO = new Color(244, 247, 250);
    private static final Color CLARO_NAVEGACION = new Color(236, 242, 247);
    private static final Color CLARO_TARJETA = Color.WHITE;
    private static final Color CLARO_BORDE = new Color(215, 224, 233);
    private static final Color CLARO_TEXTO = new Color(35, 51, 67);
    private static final Color CLARO_SECUNDARIO = new Color(100, 117, 135);
    private static final Color CLARO_SELECCION = new Color(216, 231, 241);

    private static final Color AZUL_PRINCIPAL = new Color(54, 125, 170);

    // =====================================================
    // CONTENEDORES Y TARJETAS VINCULADOS DESDE .FORM
    // =====================================================
    private JPanel panelPrincipal;
    private JScrollPane scrollContenido;
    private JPanel panelContenido;
    private JPanel panelEncabezado;
    private JPanel cardEncabezadoPrestamo;
    private JPanel cardAgregarEquipos;
    private JPanel panelControlesEquipo;
    private JPanel cardDetallePrestamo;
    private JPanel panelAcciones;
    private JPanel cardHistorialPrestamos;
    private JPanel panelBusqueda;

    private JScrollPane scrollObservaciones;
    private JScrollPane scrollDetalle;
    private JScrollPane scrollPrestamos;

    // =====================================================
    // ETIQUETAS DE SECCIÓN Y TEMA
    // =====================================================
    private JLabel lblTitulo;
    private JLabel lblSubtitulo;
    private JLabel lblSeccionEncabezado;
    private JLabel lblSeccionAgregar;
    private JLabel lblSeccionDetalle;
    private JLabel lblSeccionHistorial;
    private JButton btnModoClaro;
    private JButton btnModoOscuro;

    // =====================================================
    // COMPONENTES FUNCIONALES ORIGINALES
    // =====================================================
    private JTextField txtNoPrestamo;
    private JTextField txtEstado;
    private JComboBox cmbSolicitante;
    private JTextField txtUsuarioRegistra;
    private JTextField txtFechaPrestamo;
    private JTextField txtFechaEsperadaDevolucion;
    private JTextArea txaObservaciones;
    private JTextField txtCodigoEquipo;
    private JButton btnLeerQr;
    private JButton btnAgregarEquipo;
    private JButton btnQuitarEquipo;
    private JLabel lblEquipoIdentificado;
    private JTable tblDetalle;
    private JButton btnNuevo;
    private JButton btnConfirmarPrestamo;
    private JButton btnLimpiar;
    private JButton btnRegresar;
    private JTextField txtBuscar;
    private JComboBox cmbFiltroEstado;
    private JButton btnBuscar;
    private JTable tblPrestamos;

    // =====================================================
    // ESTADO Y CONTROLADOR DEL MÓDULO
    // =====================================================
    private final JFrame ventana;
    private final DefaultTableModel modeloDetalle;
    private final DefaultTableModel modeloPrestamos;
    private final PrestamoController controller = new PrestamoController();

    // Equipos que se van agregando al préstamo nuevo (se guardan todos juntos al confirmar)
    private final List<Equipo> equiposAgregados = new ArrayList<>();
    private List<Prestamo> prestamosEnTabla = List.of();

    public FrmPrestamos(JFrame menuPrincipal) {
        ventana = new JFrame("SIGELAB - Préstamos de Equipo De Computo");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ventana.setMinimumSize(new Dimension(920, 620));
        ventana.setExtendedState(JFrame.MAXIMIZED_BOTH);

        modeloDetalle = TablaUtil.configurar(tblDetalle, "ID", "Código", "Equipo", "Tipo", "Laboratorio", "Estado");
        modeloPrestamos = TablaUtil.configurar(tblPrestamos, "No.", "Solicitante", "Fecha préstamo", "Fecha esperada",
                "Registrado por", "Estado", "Pendientes");

        configurarScroll();

        // Al cerrar esta ventana (con Regresar o con la X) se vuelve a mostrar el menú
        ventana.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                menuPrincipal.setVisible(true);
            }
        });
        btnRegresar.addActionListener(e -> ventana.dispose());

        // Selector de tema Claro / Oscuro sincronizado con el resto del sistema
        btnModoClaro.addActionListener(e -> cambiarTema(false));
        btnModoOscuro.addActionListener(e -> cambiarTema(true));

        btnNuevo.addActionListener(e -> nuevo());
        btnLimpiar.addActionListener(e -> nuevo());
        btnAgregarEquipo.addActionListener(e -> ejecutar(this::agregarEquipo));
        txtCodigoEquipo.addActionListener(e -> btnAgregarEquipo.doClick());   // Enter (o lector de QR tipo teclado)
        btnLeerQr.addActionListener(e -> ejecutar(this::leerQr));
        btnQuitarEquipo.addActionListener(e -> ejecutar(this::quitarEquipo));
        btnConfirmarPrestamo.addActionListener(e -> ejecutar(this::confirmar));
        btnBuscar.addActionListener(e -> ejecutar(this::buscar));
        txtBuscar.addActionListener(e -> btnBuscar.doClick());
        tblPrestamos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ejecutar(this::verPrestamoSeleccionado);
            }
        });

        aplicarTema();

        ejecutar(() -> {
            DefaultComboBoxModel<Object> solicitantes = new DefaultComboBoxModel<>();
            controller.solicitantesActivos().forEach(solicitantes::addElement);
            cmbSolicitante.setModel(solicitantes);
            buscar();
        });
        nuevo();
    }

    public void mostrar() {
        aplicarTema();
        ventana.setExtendedState(JFrame.MAXIMIZED_BOTH);
        ventana.setVisible(true);
    }

    // =====================================================
    // LÓGICA DE NEGOCIO ORIGINAL INTACTA
    // =====================================================

    // Prepara el formulario para registrar un préstamo nuevo
    private void nuevo() {
        tblPrestamos.clearSelection();
        equiposAgregados.clear();
        modeloDetalle.setRowCount(0);
        txtNoPrestamo.setText("");
        txtEstado.setText("NUEVO");
        txtUsuarioRegistra.setText(Sesion.getUsuarioActual() == null ? "" : Sesion.getUsuarioActual().getNombreUsuario());
        txtFechaPrestamo.setText(Fechas.formatear(LocalDateTime.now()));
        txtFechaEsperadaDevolucion.setText(Fechas.formatear(LocalDate.now().plusDays(DIAS_PRESTAMO_SUGERIDOS)));
        txaObservaciones.setText("");
        txtCodigoEquipo.setText("");
        lblEquipoIdentificado.setText("-");
        if (cmbSolicitante.getItemCount() > 0) {
            cmbSolicitante.setSelectedIndex(0);
        }
        habilitarEdicion(true);
        txtCodigoEquipo.requestFocus();
    }

    private void agregarEquipo() {
        if (!txtNoPrestamo.getText().isBlank()) {
            throw new BusinessException("Está viendo un préstamo ya registrado. Presione \"Nuevo\" para crear otro.");
        }
        String codigo = txtCodigoEquipo.getText();
        // 1) Identificar y mostrar los datos reales del equipo (aunque luego se rechace)
        Equipo identificado = controller.identificarEquipo(codigo);
        lblEquipoIdentificado.setText(identificado.getCodigoInventario() + " - " + identificado.getNombre()
                + "  |  Estado: " + identificado.getEstado());
        // 2) El Service decide si puede agregarse (DISPONIBLE y no repetido)
        Equipo equipo = controller.validarEquipoParaPrestamo(codigo, equiposAgregados);
        equiposAgregados.add(equipo);
        modeloDetalle.addRow(new Object[]{equipo.getIdEquipo(), equipo.getCodigoInventario(), equipo.getNombre(),
                equipo.getTipoEquipo().getNombre(), equipo.getLaboratorio().getCodigo(), equipo.getEstado()});
        lblEquipoIdentificado.setText(lblEquipoIdentificado.getText() + "  ->  agregado al préstamo");
        txtCodigoEquipo.setText("");
        txtCodigoEquipo.requestFocus();
    }

    // Lee el QR desde una imagen; el código obtenido se procesa igual que si se hubiera escrito
    private void leerQr() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Seleccione la imagen del código QR");
        selector.setFileFilter(new FileNameExtensionFilter("Imágenes (png, jpg)", "png", "jpg", "jpeg"));
        if (selector.showOpenDialog(ventana) == JFileChooser.APPROVE_OPTION) {
            txtCodigoEquipo.setText(controller.leerCodigoQr(selector.getSelectedFile()));
            agregarEquipo();
        }
    }

    private void quitarEquipo() {
        int fila = tblDetalle.getSelectedRow();
        if (!txtNoPrestamo.getText().isBlank()) {
            throw new BusinessException("No se pueden quitar equipos de un préstamo ya registrado.");
        }
        if (fila < 0) {
            throw new BusinessException("Seleccione en la tabla del detalle el equipo que desea quitar.");
        }
        equiposAgregados.remove(fila);
        modeloDetalle.removeRow(fila);
    }

    private void confirmar() {
        if (!txtNoPrestamo.getText().isBlank()) {
            throw new BusinessException("Este préstamo ya fue registrado. Presione \"Nuevo\" para crear otro.");
        }
        Prestamo prestamo = new Prestamo();
        prestamo.setSolicitante((Solicitante) cmbSolicitante.getSelectedItem());
        // Se espera la devolución hasta el final del día indicado
        LocalDate fechaEsperada = Fechas.leer(txtFechaEsperadaDevolucion.getText(), "fecha esperada de devolución");
        prestamo.setFechaEsperadaDevolucion(fechaEsperada.atTime(LocalTime.of(23, 59)));
        prestamo.setObservaciones(txaObservaciones.getText());

        int idPrestamo = controller.confirmar(prestamo, new ArrayList<>(equiposAgregados));
        Dialogos.info(ventana, "Préstamo No. " + idPrestamo + " registrado.\n"
                + equiposAgregados.size() + " equipo(s) cambiaron a PRESTADO.");
        nuevo();
        buscar();
    }

    private void buscar() {
        Object filtro = cmbFiltroEstado.getSelectedItem();
        mostrarPrestamos(controller.listar(txtBuscar.getText(), filtro == null ? null : filtro.toString()));
    }

    private void mostrarPrestamos(List<Prestamo> prestamos) {
        prestamosEnTabla = prestamos;
        modeloPrestamos.setRowCount(0);
        for (Prestamo p : prestamos) {
            modeloPrestamos.addRow(new Object[]{p.getIdPrestamo(), p.getSolicitante().getNombre(),
                    Fechas.formatear(p.getFechaPrestamo()), Fechas.formatear(p.getFechaEsperadaDevolucion()),
                    p.getUsuarioRegistra(), p.isVencido() ? "VENCIDO" : p.getEstado().name(),
                    p.getEquiposPendientes() + " de " + p.getTotalEquipos()});
        }
    }

    // Muestra (solo lectura) el encabezado y el detalle del préstamo seleccionado
    private void verPrestamoSeleccionado() {
        int fila = tblPrestamos.getSelectedRow();
        if (fila < 0) {
            return;
        }
        Prestamo p = prestamosEnTabla.get(fila);
        equiposAgregados.clear();
        txtNoPrestamo.setText(String.valueOf(p.getIdPrestamo()));
        txtEstado.setText(p.isVencido() ? "VENCIDO" : p.getEstado().name());
        txtUsuarioRegistra.setText(p.getUsuarioRegistra());
        txtFechaPrestamo.setText(Fechas.formatear(p.getFechaPrestamo()));
        txtFechaEsperadaDevolucion.setText(Fechas.formatear(p.getFechaEsperadaDevolucion().toLocalDate()));
        txaObservaciones.setText("");
        lblEquipoIdentificado.setText("Préstamo de " + p.getSolicitante().getNombre() + " (" + p.getSolicitante().getCarne() + ")");
        modeloDetalle.setRowCount(0);
        for (DetallePrestamo d : controller.listarDetalles(p.getIdPrestamo())) {
            modeloDetalle.addRow(new Object[]{d.getIdDetallePrestamo(), d.getEquipo().getCodigoInventario(),
                    d.getEquipo().getNombre(), "", "", d.getEstado()
                    + (d.getCondicion() == null ? "" : " (" + d.getCondicion() + ")")});
        }
        habilitarEdicion(false);
    }

    private void habilitarEdicion(boolean nuevoPrestamo) {
        cmbSolicitante.setEnabled(nuevoPrestamo);
        txtFechaEsperadaDevolucion.setEditable(nuevoPrestamo);
        txaObservaciones.setEditable(nuevoPrestamo);
        txtCodigoEquipo.setEditable(nuevoPrestamo);
        btnLeerQr.setEnabled(nuevoPrestamo);
        btnAgregarEquipo.setEnabled(nuevoPrestamo);
        btnQuitarEquipo.setEnabled(nuevoPrestamo);
        btnConfirmarPrestamo.setEnabled(nuevoPrestamo);
        estilizarControlesSegunEstado();
    }

    private void ejecutar(Runnable accion) {
        Dialogos.ejecutar(ventana, accion);
    }

    // =====================================================
    // SCROLL Y ADAPTABILIDAD
    // =====================================================
    private void configurarScroll() {
        scrollContenido.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollContenido.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollContenido.getVerticalScrollBar().setUnitIncrement(20);
        scrollContenido.getHorizontalScrollBar().setUnitIncrement(20);
        scrollContenido.setBorder(null);
    }

    // =====================================================
    // TEMA CLARO / OSCURO OFICIAL SIGELAB
    // =====================================================
    private void cambiarTema(boolean modoOscuro) {
        TemaSIGELAB.establecerModoOscuro(modoOscuro);
        aplicarTema();
    }

    private void aplicarTema() {
        boolean oscuro = TemaSIGELAB.esOscuro();

        Color fondo = oscuro ? OSCURO_FONDO : CLARO_FONDO;
        Color navegacion = oscuro ? OSCURO_NAVEGACION : CLARO_NAVEGACION;
        Color tarjeta = oscuro ? OSCURO_TARJETA : CLARO_TARJETA;
        Color borde = oscuro ? OSCURO_BORDE : CLARO_BORDE;
        Color texto = oscuro ? OSCURO_TEXTO : CLARO_TEXTO;
        Color secundario = oscuro ? OSCURO_SECUNDARIO : CLARO_SECUNDARIO;
        Color seleccion = oscuro ? OSCURO_SELECCION : CLARO_SELECCION;

        // Fondos generales
        panelPrincipal.setBackground(fondo);
        panelContenido.setBackground(fondo);
        panelEncabezado.setBackground(fondo);
        panelAcciones.setBackground(fondo);
        panelControlesEquipo.setBackground(tarjeta);
        panelBusqueda.setBackground(tarjeta);

        scrollContenido.setBackground(fondo);
        scrollContenido.getViewport().setBackground(fondo);
        scrollContenido.getVerticalScrollBar().setBackground(fondo);
        scrollContenido.getHorizontalScrollBar().setBackground(fondo);

        // Etiquetas generales en modo secundario
        aplicarTextoBase(panelContenido, secundario);

        // Tarjetas independientes
        JPanel[] tarjetas = {
                cardEncabezadoPrestamo,
                cardAgregarEquipos,
                cardDetallePrestamo,
                cardHistorialPrestamos
        };
        for (JPanel card : tarjetas) {
            configurarTarjeta(card, tarjeta, borde);
        }

        // Encabezado del módulo
        lblTitulo.setFont(fuente(Font.BOLD, 28));
        lblTitulo.setForeground(texto);

        lblSubtitulo.setFont(fuente(Font.PLAIN, 12));
        lblSubtitulo.setForeground(secundario);

        // Títulos de secciones dentro de tarjetas
        JLabel[] titulosSeccion = {
                lblSeccionEncabezado,
                lblSeccionAgregar,
                lblSeccionDetalle,
                lblSeccionHistorial
        };
        for (JLabel sec : titulosSeccion) {
            sec.setFont(fuente(Font.BOLD, 12));
            sec.setForeground(texto);
        }

        // Etiqueta de equipo identificado destacada
        lblEquipoIdentificado.setFont(fuente(Font.BOLD, 13));
        lblEquipoIdentificado.setForeground(oscuro ? new Color(118, 185, 228) : AZUL_PRINCIPAL);

        // Botones Sol / Luna
        configurarBotonTema(btnModoClaro, !oscuro, tarjeta, texto, borde);
        configurarBotonTema(btnModoOscuro, oscuro, tarjeta, texto, borde);

        // Campos de texto, ComboBox y TextArea
        JTextField[] campos = {
                txtNoPrestamo,
                txtEstado,
                txtUsuarioRegistra,
                txtFechaPrestamo,
                txtFechaEsperadaDevolucion,
                txtCodigoEquipo,
                txtBuscar
        };
        for (JTextField campo : campos) {
            configurarCampoTexto(campo, oscuro, navegacion, tarjeta, texto, secundario, borde);
        }

        configurarComboBox(cmbSolicitante, oscuro, navegacion, texto, borde);
        configurarComboBox(cmbFiltroEstado, oscuro, navegacion, texto, borde);
        configurarAreaTexto(txaObservaciones, scrollObservaciones, oscuro, navegacion, texto, borde);

        // Botones principales (Azul oficial #367DAA)
        configurarBotonPrincipal(btnConfirmarPrestamo);
        configurarBotonPrincipal(btnAgregarEquipo);
        configurarBotonPrincipal(btnBuscar);

        // Botones secundarios (Neutros y discretos)
        configurarBotonSecundario(btnLeerQr, oscuro, navegacion, texto, borde);
        configurarBotonSecundario(btnNuevo, oscuro, tarjeta, texto, borde);
        configurarBotonSecundario(btnLimpiar, oscuro, tarjeta, texto, borde);
        configurarBotonSecundario(btnRegresar, oscuro, tarjeta, texto, borde);

        // Botón de acción delicada (Quitar equipo del detalle)
        configurarBotonAdvertencia(btnQuitarEquipo, oscuro);

        // Tablas
        configurarTabla(tblDetalle, scrollDetalle, oscuro, tarjeta, navegacion, texto, secundario, borde, seleccion);
        configurarTabla(tblPrestamos, scrollPrestamos, oscuro, tarjeta, navegacion, texto, secundario, borde, seleccion);

        estilizarControlesSegunEstado();

        panelPrincipal.revalidate();
        panelPrincipal.repaint();
    }

    private void estilizarControlesSegunEstado() {
        boolean oscuro = TemaSIGELAB.esOscuro();
        Color fondoEditable = oscuro ? OSCURO_NAVEGACION : Color.WHITE;
        Color fondoSoloLectura = oscuro ? new Color(15, 23, 36) : CLARO_NAVEGACION;
        Color textoActivo = oscuro ? OSCURO_TEXTO : CLARO_TEXTO;
        Color textoInactivo = oscuro ? OSCURO_SECUNDARIO : CLARO_SECUNDARIO;

        JTextField[] todos = {
                txtNoPrestamo,
                txtEstado,
                txtUsuarioRegistra,
                txtFechaPrestamo,
                txtFechaEsperadaDevolucion,
                txtCodigoEquipo,
                txtBuscar
        };
        for (JTextField f : todos) {
            if (f == null) continue;
            f.setBackground(f.isEditable() ? fondoEditable : fondoSoloLectura);
            f.setForeground(f.isEditable() ? textoActivo : textoInactivo);
        }

        // Destacar visualmente el campo Estado
        txtEstado.setFont(fuente(Font.BOLD, 12));
        txtEstado.setForeground(oscuro ? new Color(118, 185, 228) : AZUL_PRINCIPAL);

        if (txaObservaciones != null) {
            txaObservaciones.setBackground(txaObservaciones.isEditable() ? fondoEditable : fondoSoloLectura);
            txaObservaciones.setForeground(txaObservaciones.isEditable() ? textoActivo : textoInactivo);
        }
    }

    // =====================================================
    // MÉTODOS AUXILIARES DE ESTILO VISUAL
    // =====================================================
    private Font fuente(int estilo, int tamano) {
        return new Font("Segoe UI", estilo, tamano);
    }

    private void configurarTarjeta(JPanel panel, Color fondo, Color borde) {
        panel.setOpaque(true);
        panel.setBackground(fondo);
        panel.setBorder(new LineBorder(borde, 1, true));
    }

    private void configurarCampoTexto(
            JTextField campo,
            boolean oscuro,
            Color fondoInput,
            Color fondoTarjeta,
            Color texto,
            Color secundario,
            Color borde
    ) {
        campo.setFont(fuente(Font.PLAIN, 13));
        campo.setCaretColor(texto);
        campo.setBackground(campo.isEditable() ? (oscuro ? fondoInput : Color.WHITE) : (oscuro ? new Color(15, 23, 36) : fondoInput));
        campo.setForeground(campo.isEditable() ? texto : secundario);
        campo.setBorder(new CompoundBorder(
                new LineBorder(borde, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
    }

    private void configurarComboBox(
            JComboBox<?> combo,
            boolean oscuro,
            Color fondoInput,
            Color texto,
            Color borde
    ) {
        combo.setFont(fuente(Font.PLAIN, 13));
        combo.setBackground(oscuro ? fondoInput : Color.WHITE);
        combo.setForeground(texto);
        combo.setBorder(new LineBorder(borde, 1, true));
    }

    private void configurarAreaTexto(
            JTextArea area,
            JScrollPane scroll,
            boolean oscuro,
            Color fondoInput,
            Color texto,
            Color borde
    ) {
        Color bg = oscuro ? fondoInput : Color.WHITE;
        area.setFont(fuente(Font.PLAIN, 13));
        area.setBackground(bg);
        area.setForeground(texto);
        area.setCaretColor(texto);
        area.setBorder(new EmptyBorder(6, 10, 6, 10));

        scroll.setBackground(bg);
        scroll.getViewport().setBackground(bg);
        scroll.setBorder(new LineBorder(borde, 1, true));
    }

    private void configurarBotonPrincipal(JButton boton) {
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setFocusPainted(false);
        boton.setBackground(AZUL_PRINCIPAL);
        boton.setForeground(Color.WHITE);
        boton.setFont(fuente(Font.BOLD, 12));
        boton.setBorder(new EmptyBorder(9, 18, 9, 18));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void configurarBotonSecundario(
            JButton boton,
            boolean oscuro,
            Color fondo,
            Color texto,
            Color borde
    ) {
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setFocusPainted(false);
        boton.setBackground(fondo);
        boton.setForeground(texto);
        boton.setFont(fuente(Font.BOLD, 12));
        boton.setBorder(new CompoundBorder(
                new LineBorder(borde, 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void configurarBotonAdvertencia(JButton boton, boolean oscuro) {
        Color fondoAdv = oscuro ? new Color(62, 34, 41) : new Color(251, 235, 235);
        Color bordeAdv = oscuro ? new Color(122, 58, 68) : new Color(218, 168, 168);
        Color textoAdv = oscuro ? new Color(235, 165, 165) : new Color(156, 52, 52);

        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setFocusPainted(false);
        boton.setBackground(fondoAdv);
        boton.setForeground(textoAdv);
        boton.setFont(fuente(Font.BOLD, 12));
        boton.setBorder(new CompoundBorder(
                new LineBorder(bordeAdv, 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void configurarBotonTema(
            JButton boton,
            boolean activo,
            Color fondo,
            Color texto,
            Color borde
    ) {
        boton.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 20));
        boton.setOpaque(true);
        boton.setFocusPainted(false);
        boton.setBackground(activo ? AZUL_PRINCIPAL : fondo);
        boton.setForeground(activo ? Color.WHITE : texto);
        boton.setBorder(new LineBorder(activo ? AZUL_PRINCIPAL : borde, 1, true));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void configurarTabla(
            JTable tabla,
            JScrollPane scroll,
            boolean oscuro,
            Color tarjeta,
            Color encabezadoFondo,
            Color texto,
            Color secundario,
            Color borde,
            Color seleccion
    ) {
        tabla.setFont(fuente(Font.PLAIN, 12));
        tabla.setRowHeight(30);
        tabla.setBackground(tarjeta);
        tabla.setForeground(texto);
        tabla.setGridColor(borde);
        tabla.setShowHorizontalLines(true);
        tabla.setShowVerticalLines(false);
        tabla.setSelectionBackground(seleccion);
        tabla.setSelectionForeground(oscuro ? Color.WHITE : texto);
        tabla.setFillsViewportHeight(true);

        JTableHeader header = tabla.getTableHeader();
        if (header != null) {
            header.setReorderingAllowed(false);
            header.setPreferredSize(new Dimension(header.getWidth(), 34));
            header.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(
                        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column
                ) {
                    JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                            table, value, isSelected, hasFocus, row, column
                    );
                    lbl.setOpaque(true);
                    lbl.setBackground(encabezadoFondo);
                    lbl.setForeground(secundario);
                    lbl.setFont(fuente(Font.BOLD, 11));
                    lbl.setBorder(new CompoundBorder(
                            new LineBorder(borde, 1, false),
                            new EmptyBorder(6, 10, 6, 10)
                    ));
                    return lbl;
                }
            });
        }

        scroll.setBackground(tarjeta);
        scroll.getViewport().setBackground(tarjeta);
        scroll.setBorder(new LineBorder(borde, 1, true));
    }

    private void aplicarTextoBase(Container contenedor, Color secundario) {
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JLabel) {
                JLabel etiqueta = (JLabel) componente;
                etiqueta.setForeground(secundario);
                etiqueta.setFont(fuente(Font.PLAIN, 12));
            }
            if (componente instanceof Container) {
                aplicarTextoBase((Container) componente, secundario);
            }
        }
    }
}