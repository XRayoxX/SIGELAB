package SIGELAB.view;

import SIGELAB.controller.SolicitanteController;
import SIGELAB.enums.EstadoRegistro;
import SIGELAB.enums.TipoSolicitante;
import SIGELAB.model.Solicitante;
import SIGELAB.util.Dialogos;
import SIGELAB.util.EstiloFormulario;
import SIGELAB.util.TablaUtil;
import SIGELAB.util.TemaSIGELAB;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class FrmSolicitantes {

    // Componentes vinculados desde FrmSolicitantes.form
    private JPanel panelPrincipal;
    private JScrollPane scrollContenido;
    private JPanel cardBusqueda;
    private JPanel cardDatos;
    private JPanel cardRegistros;
    private JLabel lblTitulo;
    private JLabel lblSubtitulo;
    private JLabel lblSeccionBusqueda;
    private JLabel lblSeccionDatos;
    private JLabel lblSeccionRegistros;
    private JLabel lblIndicaciones;
    private JButton btnModoClaro;
    private JButton btnModoOscuro;
    private JTextField txtId;
    private JTextField txtCarne;
    private JTextField txtNombre;
    private JTextField txtApellido;
    private JComboBox cmbTipoSolicitante;
    private JTextField txtCorreo;
    private JTextField txtTelefono;
    private JComboBox cmbEstado;
    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton btnLimpiar;
    private JButton btnRegresar;
    private JTextField txtBuscar;
    private JButton btnBuscar;
    private JTable tblSolicitantes;

    private final JFrame ventana;
    private final DefaultTableModel modeloTabla;
    private final SolicitanteController controller = new SolicitanteController();
    private List<Solicitante> solicitantesEnTabla = List.of();

    public FrmSolicitantes(JFrame menuPrincipal) {
        ventana = new JFrame("SIGELAB - Gestión de Solicitantes");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        modeloTabla = TablaUtil.configurar(tblSolicitantes, "ID", "Carné", "Nombre", "Apellido", "Tipo", "Correo", "Teléfono", "Estado");
        configurarEstilo();

        // Pantalla completa (maximizada), respetando la barra de tareas de Windows
        EstiloFormulario.pantallaCompleta(ventana);

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
            Solicitante s = controller.guardar(leerFormulario());
            Dialogos.info(ventana, "Solicitante " + s.getCarne() + " guardado correctamente.");
            limpiar();
            mostrarEnTabla(controller.listar());
        }));
        btnActualizar.addActionListener(e -> ejecutar(() -> {
            controller.actualizar(leerFormulario());
            Dialogos.info(ventana, "Solicitante actualizado correctamente.");
            limpiar();
            mostrarEnTabla(controller.listar());
        }));
        btnDesactivar.addActionListener(e -> {
            int id = idSeleccionado();
            if (id > 0 && !Dialogos.confirmar(ventana, "¿Desea desactivar el solicitante seleccionado?")) {
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
        tblSolicitantes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccionado();
            }
        });

        ejecutar(() -> mostrarEnTabla(controller.listar()));
    }

    public void mostrar() {
        ventana.setVisible(true);
    }

    // =====================================
    // DISEÑO SIGELAB (modo claro / oscuro)
    // =====================================

    private void configurarEstilo() {
        EstiloFormulario.configurarScroll(scrollContenido);
        EstiloFormulario.tarjeta(cardBusqueda, cardDatos, cardRegistros);
        EstiloFormulario.titulo(lblTitulo);
        EstiloFormulario.subtitulo(lblSubtitulo);
        EstiloFormulario.seccion(lblSeccionBusqueda, lblSeccionDatos, lblSeccionRegistros);
        EstiloFormulario.indicacion(lblIndicaciones);
        EstiloFormulario.botonPrincipal(btnGuardar, btnActualizar, btnBuscar);
        EstiloFormulario.botonPeligro(btnDesactivar);
        EstiloFormulario.botonesTema(btnModoClaro, btnModoOscuro);
        btnModoClaro.addActionListener(e -> cambiarTema(false));
        btnModoOscuro.addActionListener(e -> cambiarTema(true));
        aplicarTema();
    }

    private void cambiarTema(boolean modoOscuro) {
        TemaSIGELAB.establecerModoOscuro(modoOscuro);
        aplicarTema();
    }

    private void aplicarTema() {
        EstiloFormulario.aplicar(panelPrincipal);
    }

    private Solicitante leerFormulario() {
        Solicitante s = new Solicitante();
        s.setIdSolicitante(idSeleccionado());
        s.setCarne(txtCarne.getText());
        s.setNombre(txtNombre.getText());
        s.setApellido(txtApellido.getText());
        s.setTipo(TipoSolicitante.valueOf(String.valueOf(cmbTipoSolicitante.getSelectedItem())));
        s.setCorreo(txtCorreo.getText());
        s.setTelefono(txtTelefono.getText());
        s.setEstado(EstadoRegistro.valueOf(String.valueOf(cmbEstado.getSelectedItem())));
        return s;
    }

    private int idSeleccionado() {
        return txtId.getText().isBlank() ? 0 : Integer.parseInt(txtId.getText());
    }

    private void mostrarEnTabla(List<Solicitante> solicitantes) {
        solicitantesEnTabla = solicitantes;
        modeloTabla.setRowCount(0);
        for (Solicitante s : solicitantes) {
            modeloTabla.addRow(new Object[]{s.getIdSolicitante(), s.getCarne(), s.getNombre(), s.getApellido(),
                    s.getTipo(), s.getCorreo(), s.getTelefono(), s.getEstado()});
        }
    }

    private void cargarSeleccionado() {
        int fila = tblSolicitantes.getSelectedRow();
        if (fila < 0) {
            return;
        }
        Solicitante s = solicitantesEnTabla.get(fila);
        txtId.setText(String.valueOf(s.getIdSolicitante()));
        txtCarne.setText(s.getCarne());
        txtNombre.setText(s.getNombre());
        txtApellido.setText(s.getApellido());
        cmbTipoSolicitante.setSelectedItem(s.getTipo().name());
        txtCorreo.setText(s.getCorreo() == null ? "" : s.getCorreo());
        txtTelefono.setText(s.getTelefono() == null ? "" : s.getTelefono());
        cmbEstado.setSelectedItem(s.getEstado().name());
    }

    private void limpiar() {
        tblSolicitantes.clearSelection();
        txtId.setText("");
        txtCarne.setText("");
        txtNombre.setText("");
        txtApellido.setText("");
        txtCorreo.setText("");
        txtTelefono.setText("");
        cmbTipoSolicitante.setSelectedIndex(0);
        cmbEstado.setSelectedItem(EstadoRegistro.ACTIVO.name());
        txtCarne.requestFocus();
    }

    private void ejecutar(Runnable accion) {
        Dialogos.ejecutar(ventana, accion);
    }
}
