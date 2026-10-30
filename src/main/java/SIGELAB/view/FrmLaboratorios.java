package SIGELAB.view;

import SIGELAB.controller.LaboratorioController;
import SIGELAB.enums.EstadoRegistro;
import SIGELAB.model.Laboratorio;
import SIGELAB.util.Dialogos;
import SIGELAB.util.TablaUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import SIGELAB.util.EstiloFormulario;
import SIGELAB.util.TemaSIGELAB;

public class FrmLaboratorios {

    // Componentes vinculados desde FrmLaboratorios.form
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
    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JTextField txtUbicacion;
    private JSpinner spnCapacidad;
    private JComboBox cmbEstado;
    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton btnLimpiar;
    private JButton btnRegresar;
    private JTextField txtBuscar;
    private JButton btnBuscar;
    private JTable tblLaboratorios;

    private final JFrame ventana;
    private final DefaultTableModel modeloTabla;
    private final LaboratorioController controller = new LaboratorioController();

    // Laboratorios que se están mostrando en la tabla (misma posición que cada fila)
    private List<Laboratorio> laboratoriosEnTabla = List.of();

    public FrmLaboratorios(JFrame menuPrincipal) {
        ventana = new JFrame("SIGELAB - Gestión de Laboratorios");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Encabezados de las tablas (los datos se cargan desde el Controller)
        modeloTabla = TablaUtil.configurar(tblLaboratorios, "ID", "Código", "Nombre", "Ubicación", "Capacidad", "Estado");

        // La capacidad siempre debe ser un número positivo
        spnCapacidad.setModel(new SpinnerNumberModel(1, 1, 1000, 1));

        // Aplicar la paleta oficial SIGELAB.
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
        btnGuardar.addActionListener(e -> guardar());
        btnActualizar.addActionListener(e -> actualizar());
        btnDesactivar.addActionListener(e -> desactivar());
        btnBuscar.addActionListener(e -> ejecutar(() -> mostrarEnTabla(controller.buscar(txtBuscar.getText()))));
        txtBuscar.addActionListener(e -> btnBuscar.doClick());   // Enter en el buscador
        tblLaboratorios.getSelectionModel().addListSelectionListener(e -> {
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

    private void guardar() {
        ejecutar(() -> {
            Laboratorio lab = controller.guardar(leerFormulario());
            JOptionPane.showMessageDialog(ventana, "Laboratorio " + lab.getCodigo() + " guardado correctamente.");
            limpiar();
            mostrarEnTabla(controller.listar());
        });
    }

    private void actualizar() {
        ejecutar(() -> {
            controller.actualizar(leerFormulario());
            JOptionPane.showMessageDialog(ventana, "Laboratorio actualizado correctamente.");
            limpiar();
            mostrarEnTabla(controller.listar());
        });
    }

    private void desactivar() {
        int id = idSeleccionado();
        if (id > 0 && JOptionPane.showConfirmDialog(ventana, "¿Desea desactivar el laboratorio seleccionado?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        ejecutar(() -> {
            controller.desactivar(id);
            limpiar();
            mostrarEnTabla(controller.listar());
        });
    }

    // Convierte lo escrito en el formulario en un objeto Laboratorio
    private Laboratorio leerFormulario() {
        Laboratorio lab = new Laboratorio();
        lab.setIdLaboratorio(idSeleccionado());
        lab.setCodigo(txtCodigo.getText());
        lab.setNombre(txtNombre.getText());
        lab.setUbicacion(txtUbicacion.getText());
        lab.setCapacidad((Integer) spnCapacidad.getValue());
        lab.setEstado(EstadoRegistro.valueOf(String.valueOf(cmbEstado.getSelectedItem())));
        return lab;
    }

    private int idSeleccionado() {
        return txtId.getText().isBlank() ? 0 : Integer.parseInt(txtId.getText());
    }

    private void mostrarEnTabla(List<Laboratorio> laboratorios) {
        laboratoriosEnTabla = laboratorios;
        modeloTabla.setRowCount(0);
        for (Laboratorio lab : laboratorios) {
            modeloTabla.addRow(new Object[]{
                    lab.getIdLaboratorio(), lab.getCodigo(), lab.getNombre(),
                    lab.getUbicacion(), lab.getCapacidad(), lab.getEstado()});
        }
    }

    private void cargarSeleccionado() {
        int fila = tblLaboratorios.getSelectedRow();
        if (fila < 0) {
            return;
        }
        Laboratorio lab = laboratoriosEnTabla.get(fila);
        txtId.setText(String.valueOf(lab.getIdLaboratorio()));
        txtCodigo.setText(lab.getCodigo());
        txtNombre.setText(lab.getNombre());
        txtUbicacion.setText(lab.getUbicacion());
        spnCapacidad.setValue(lab.getCapacidad());
        cmbEstado.setSelectedItem(lab.getEstado().name());
    }

    private void ejecutar(Runnable accion) {
        Dialogos.ejecutar(ventana, accion);
    }

    private void limpiar() {
        tblLaboratorios.clearSelection();
        txtId.setText("");
        txtCodigo.setText("");
        txtNombre.setText("");
        txtUbicacion.setText("");
        spnCapacidad.setValue(1);
        cmbEstado.setSelectedItem(EstadoRegistro.ACTIVO.name());
        txtCodigo.requestFocus();
    }
}
