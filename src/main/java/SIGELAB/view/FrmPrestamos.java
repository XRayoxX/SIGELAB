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

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class FrmPrestamos {

    private static final int DIAS_PRESTAMO_SUGERIDOS = 3;

    // Componentes vinculados desde FrmPrestamos.form
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
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

        modeloDetalle = TablaUtil.configurar(tblDetalle, "ID", "Código", "Equipo", "Tipo", "Laboratorio", "Estado");
        modeloPrestamos = TablaUtil.configurar(tblPrestamos, "No.", "Solicitante", "Fecha préstamo", "Fecha esperada",
                "Registrado por", "Estado", "Pendientes");

        ventana.pack();
        ventana.setLocationRelativeTo(null);

        // Al cerrar esta ventana (con Regresar o con la X) se vuelve a mostrar el menú
        ventana.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                menuPrincipal.setVisible(true);
            }
        });
        btnRegresar.addActionListener(e -> ventana.dispose());

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

        ejecutar(() -> {
            DefaultComboBoxModel<Object> solicitantes = new DefaultComboBoxModel<>();
            controller.solicitantesActivos().forEach(solicitantes::addElement);
            cmbSolicitante.setModel(solicitantes);
            buscar();
        });
        nuevo();
    }

    public void mostrar() {
        ventana.setVisible(true);
    }

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
    }

    private void ejecutar(Runnable accion) {
        Dialogos.ejecutar(ventana, accion);
    }
}
