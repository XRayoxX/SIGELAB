package SIGELAB.view;

import SIGELAB.controller.TipoEquipoController;
import SIGELAB.enums.EstadoRegistro;
import SIGELAB.model.TipoEquipo;
import SIGELAB.util.Dialogos;
import SIGELAB.util.TablaUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class FrmTiposEquipo {

    // Componentes vinculados desde FrmTiposEquipo.form
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JTextField txtId;
    private JTextField txtNombre;
    private JComboBox cmbEstado;
    private JTextArea txaDescripcion;
    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton btnLimpiar;
    private JButton btnRegresar;
    private JTextField txtBuscar;
    private JButton btnBuscar;
    private JTable tblTiposEquipo;

    private final JFrame ventana;
    private final DefaultTableModel modeloTabla;
    private final TipoEquipoController controller = new TipoEquipoController();
    private List<TipoEquipo> tiposEnTabla = List.of();

    public FrmTiposEquipo(JFrame menuPrincipal) {
        ventana = new JFrame("SIGELAB - Tipos de Equipo");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        modeloTabla = TablaUtil.configurar(tblTiposEquipo, "ID", "Nombre", "Descripción", "Estado");

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

        btnNuevo.addActionListener(e -> limpiar());
        btnLimpiar.addActionListener(e -> limpiar());
        btnGuardar.addActionListener(e -> ejecutar(() -> {
            TipoEquipo tipo = controller.guardar(leerFormulario());
            Dialogos.info(ventana, "Tipo de equipo " + tipo.getNombre() + " guardado correctamente.");
            limpiar();
            mostrarEnTabla(controller.listar());
        }));
        btnActualizar.addActionListener(e -> ejecutar(() -> {
            controller.actualizar(leerFormulario());
            Dialogos.info(ventana, "Tipo de equipo actualizado correctamente.");
            limpiar();
            mostrarEnTabla(controller.listar());
        }));
        btnDesactivar.addActionListener(e -> {
            int id = idSeleccionado();
            if (id > 0 && !Dialogos.confirmar(ventana, "¿Desea desactivar el tipo de equipo seleccionado?")) {
                return;
            }
            ejecutar(() -> {
                controller.desactivar(id);
                limpiar();
                mostrarEnTabla(controller.listar());
            });
        });
        btnBuscar.addActionListener(e -> ejecutar(() -> mostrarEnTabla(controller.buscar(txtBuscar.getText()))));
        txtBuscar.addActionListener(e -> btnBuscar.doClick());
        tblTiposEquipo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccionado();
            }
        });

        ejecutar(() -> mostrarEnTabla(controller.listar()));
    }

    public void mostrar() {
        ventana.setVisible(true);
    }

    private TipoEquipo leerFormulario() {
        TipoEquipo tipo = new TipoEquipo();
        tipo.setIdTipoEquipo(idSeleccionado());
        tipo.setNombre(txtNombre.getText());
        tipo.setDescripcion(txaDescripcion.getText());
        tipo.setEstado(EstadoRegistro.valueOf(String.valueOf(cmbEstado.getSelectedItem())));
        return tipo;
    }

    private int idSeleccionado() {
        return txtId.getText().isBlank() ? 0 : Integer.parseInt(txtId.getText());
    }

    private void mostrarEnTabla(List<TipoEquipo> tipos) {
        tiposEnTabla = tipos;
        modeloTabla.setRowCount(0);
        for (TipoEquipo t : tipos) {
            modeloTabla.addRow(new Object[]{t.getIdTipoEquipo(), t.getNombre(), t.getDescripcion(), t.getEstado()});
        }
    }

    private void cargarSeleccionado() {
        int fila = tblTiposEquipo.getSelectedRow();
        if (fila < 0) {
            return;
        }
        TipoEquipo t = tiposEnTabla.get(fila);
        txtId.setText(String.valueOf(t.getIdTipoEquipo()));
        txtNombre.setText(t.getNombre());
        txaDescripcion.setText(t.getDescripcion() == null ? "" : t.getDescripcion());
        cmbEstado.setSelectedItem(t.getEstado().name());
    }

    private void limpiar() {
        tblTiposEquipo.clearSelection();
        txtId.setText("");
        txtNombre.setText("");
        txaDescripcion.setText("");
        cmbEstado.setSelectedItem(EstadoRegistro.ACTIVO.name());
        txtNombre.requestFocus();
    }

    private void ejecutar(Runnable accion) {
        Dialogos.ejecutar(ventana, accion);
    }
}
