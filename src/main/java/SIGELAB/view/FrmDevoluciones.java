package SIGELAB.view;

import SIGELAB.controller.DevolucionController;
import SIGELAB.enums.CondicionDevolucion;
import SIGELAB.enums.EstadoDetalle;
import SIGELAB.model.DetallePrestamo;
import SIGELAB.model.Prestamo;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Dialogos;
import SIGELAB.util.Fechas;
import SIGELAB.util.TablaUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class FrmDevoluciones {

    // Componentes vinculados desde FrmDevoluciones.form
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JTextField txtBuscar;
    private JButton btnBuscar;
    private JTable tblPrestamosActivos;
    private JTextField txtNoPrestamo;
    private JTextField txtSolicitante;
    private JTextField txtFechaPrestamo;
    private JTextField txtFechaEsperadaDevolucion;
    private JTextField txtDiasAtraso;
    private JTextField txtEquiposPendientes;
    private JTable tblEquiposPrestamo;
    private JTextField txtEquipo;
    private JComboBox cmbCondicion;
    private JTextField txtFechaDevolucion;
    private JTextArea txaObservaciones;
    private JButton btnRegistrarDevolucion;
    private JButton btnLimpiar;
    private JButton btnRegresar;

    private final JFrame ventana;
    private final DefaultTableModel modeloPrestamos;
    private final DefaultTableModel modeloEquipos;
    private final DevolucionController controller = new DevolucionController();
    private List<Prestamo> prestamosEnTabla = List.of();
    private List<DetallePrestamo> equiposEnTabla = List.of();

    public FrmDevoluciones(JFrame menuPrincipal) {
        ventana = new JFrame("SIGELAB - Devolución de Equipo");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        modeloPrestamos = TablaUtil.configurar(tblPrestamosActivos, "No.", "Solicitante", "Fecha préstamo", "Fecha esperada", "Pendientes", "Vencido");
        modeloEquipos = TablaUtil.configurar(tblEquiposPrestamo, "ID detalle", "Código", "Equipo", "Estado", "Condición", "Fecha devolución");

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

        btnBuscar.addActionListener(e -> ejecutar(this::buscar));
        txtBuscar.addActionListener(e -> btnBuscar.doClick());
        btnLimpiar.addActionListener(e -> limpiar());
        btnRegistrarDevolucion.addActionListener(e -> ejecutar(this::registrarDevolucion));
        tblPrestamosActivos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ejecutar(this::cargarPrestamoSeleccionado);
            }
        });
        tblEquiposPrestamo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarEquipoSeleccionado();
            }
        });

        limpiar();
        ejecutar(this::buscar);
    }

    public void mostrar() {
        ventana.setVisible(true);
    }

    private void buscar() {
        prestamosEnTabla = controller.prestamosActivos(txtBuscar.getText());
        modeloPrestamos.setRowCount(0);
        for (Prestamo p : prestamosEnTabla) {
            modeloPrestamos.addRow(new Object[]{p.getIdPrestamo(), p.getSolicitante().getNombre(),
                    Fechas.formatear(p.getFechaPrestamo()), Fechas.formatear(p.getFechaEsperadaDevolucion()),
                    p.getEquiposPendientes() + " de " + p.getTotalEquipos(), p.isVencido() ? "SÍ" : "NO"});
        }
    }

    private void cargarPrestamoSeleccionado() {
        int fila = tblPrestamosActivos.getSelectedRow();
        if (fila < 0) {
            return;
        }
        Prestamo p = prestamosEnTabla.get(fila);
        txtNoPrestamo.setText(String.valueOf(p.getIdPrestamo()));
        txtSolicitante.setText(p.getSolicitante().getNombre() + " (" + p.getSolicitante().getCarne() + ")");
        txtFechaPrestamo.setText(Fechas.formatear(p.getFechaPrestamo()));
        txtFechaEsperadaDevolucion.setText(Fechas.formatear(p.getFechaEsperadaDevolucion()));
        long atraso = ChronoUnit.DAYS.between(p.getFechaEsperadaDevolucion().toLocalDate(), LocalDate.now());
        txtDiasAtraso.setText(atraso > 0 ? String.valueOf(atraso) : "0");
        txtEquiposPendientes.setText(p.getEquiposPendientes() + " de " + p.getTotalEquipos());
        mostrarEquipos(p.getIdPrestamo());
    }

    private void mostrarEquipos(int idPrestamo) {
        equiposEnTabla = controller.equiposDelPrestamo(idPrestamo);
        modeloEquipos.setRowCount(0);
        for (DetallePrestamo d : equiposEnTabla) {
            modeloEquipos.addRow(new Object[]{d.getIdDetallePrestamo(), d.getEquipo().getCodigoInventario(),
                    d.getEquipo().getNombre(), d.getEstado(), d.getCondicion() == null ? "" : d.getCondicion(),
                    Fechas.formatear(d.getFechaDevolucion())});
        }
        limpiarDevolucion();
    }

    private void cargarEquipoSeleccionado() {
        int fila = tblEquiposPrestamo.getSelectedRow();
        if (fila < 0) {
            return;
        }
        DetallePrestamo d = equiposEnTabla.get(fila);
        txtEquipo.setText(d.getEquipo().getCodigoInventario() + " - " + d.getEquipo().getNombre());
        boolean pendiente = d.getEstado() == EstadoDetalle.PENDIENTE;
        txtFechaDevolucion.setText(pendiente ? Fechas.formatear(LocalDateTime.now()) : Fechas.formatear(d.getFechaDevolucion()));
        txaObservaciones.setText(pendiente || d.getObservacionDevolucion() == null ? "" : d.getObservacionDevolucion());
        if (!pendiente) {
            cmbCondicion.setSelectedItem(d.getCondicion().name());
        }
    }

    private void registrarDevolucion() {
        int fila = tblEquiposPrestamo.getSelectedRow();
        if (fila < 0) {
            throw new BusinessException("Seleccione en la tabla el equipo que se está devolviendo.");
        }
        DetallePrestamo d = equiposEnTabla.get(fila);
        CondicionDevolucion condicion = CondicionDevolucion.valueOf(String.valueOf(cmbCondicion.getSelectedItem()));
        int idPrestamo = d.getIdPrestamo();

        // La regla "no devolver dos veces" la valida el Service, aunque el equipo ya aparezca DEVUELTO aquí
        int pendientes = controller.registrar(d.getIdDetallePrestamo(), condicion, txaObservaciones.getText());

        String estadoEquipo = condicion == CondicionDevolucion.BUENO ? "DISPONIBLE" : "DANADO";
        if (pendientes == 0) {
            Dialogos.info(ventana, "Equipo " + d.getEquipo().getCodigoInventario() + " devuelto (" + estadoEquipo + ").\n"
                    + "Ya no quedan equipos pendientes: el préstamo No. " + idPrestamo + " quedó FINALIZADO.");
            limpiar();
            buscar();
        } else {
            Dialogos.info(ventana, "Equipo " + d.getEquipo().getCodigoInventario() + " devuelto (" + estadoEquipo + ").\n"
                    + "El préstamo sigue ACTIVO con " + pendientes + " equipo(s) pendiente(s).");
            buscar();
            seleccionarPrestamo(idPrestamo);
        }
    }

    private void seleccionarPrestamo(int idPrestamo) {
        for (int i = 0; i < prestamosEnTabla.size(); i++) {
            if (prestamosEnTabla.get(i).getIdPrestamo() == idPrestamo) {
                tblPrestamosActivos.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    private void limpiarDevolucion() {
        tblEquiposPrestamo.clearSelection();
        txtEquipo.setText("");
        txtFechaDevolucion.setText("");
        txaObservaciones.setText("");
        cmbCondicion.setSelectedItem(CondicionDevolucion.BUENO.name());
    }

    private void limpiar() {
        tblPrestamosActivos.clearSelection();
        txtNoPrestamo.setText("");
        txtSolicitante.setText("");
        txtFechaPrestamo.setText("");
        txtFechaEsperadaDevolucion.setText("");
        txtDiasAtraso.setText("");
        txtEquiposPendientes.setText("");
        equiposEnTabla = List.of();
        modeloEquipos.setRowCount(0);
        limpiarDevolucion();
    }

    private void ejecutar(Runnable accion) {
        Dialogos.ejecutar(ventana, accion);
    }
}
