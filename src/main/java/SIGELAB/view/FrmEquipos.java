package SIGELAB.view;

import SIGELAB.controller.EquipoController;
import SIGELAB.enums.EstadoEquipo;
import SIGELAB.model.Equipo;
import SIGELAB.model.Laboratorio;
import SIGELAB.model.TipoEquipo;
import SIGELAB.util.BusinessException;
import SIGELAB.util.Dialogos;
import SIGELAB.util.TablaUtil;
import SIGELAB.util.EstiloFormulario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class FrmEquipos {

    private static final String TODOS = "TODOS";
    private static final int TAMANO_QR = 150;

    // Componentes vinculados desde FrmEquipos.form
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JTextField txtId;
    private JTextField txtCodigoInventario;
    private JTextField txtNombre;
    private JComboBox cmbTipoEquipo;
    private JTextField txtMarca;
    private JTextField txtModelo;
    private JTextField txtNumeroSerie;
    private JComboBox cmbLaboratorio;
    private JComboBox cmbEstado;
    private JTextArea txaObservaciones;
    private JLabel lblCodigoQr;
    private JButton btnGenerarQr;
    private JButton btnGuardarQr;
    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnVerHistorial;
    private JButton btnLimpiar;
    private JButton btnRegresar;
    private JTextField txtBuscar;
    private JComboBox cmbFiltroEstado;
    private JComboBox cmbFiltroLaboratorio;
    private JButton btnBuscar;
    private JTable tblEquipos;

    private final JFrame ventana;
    private final DefaultTableModel modeloTabla;
    private final EquipoController controller = new EquipoController();
    private List<Equipo> equiposEnTabla = List.of();
    private BufferedImage qrActual;

    public FrmEquipos(JFrame menuPrincipal) {
        ventana = new JFrame("SIGELAB - Gestión de Equipos");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);


        modeloTabla = TablaUtil.configurar(
                tblEquipos,
                "ID",
                "Código",
                "Nombre",
                "Tipo",
                "Marca",
                "Modelo",
                "No. serie",
                "Laboratorio",
                "Estado"
        );

// =====================================
// APLICAR TEMA SIGELAB DARK TECH
// =====================================

        EstiloFormulario.aplicar(
                panelPrincipal
        );

// =====================================
// CONFIGURAR VENTANA
// =====================================

        ventana.pack();

// Abrir maximizada respetando la
// barra de tareas de Windows.
        ventana.setExtendedState(
                JFrame.MAXIMIZED_BOTH
        );


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
            Equipo equipo = controller.guardar(leerFormulario());
            Dialogos.info(ventana, "Equipo " + equipo.getCodigoInventario() + " registrado como DISPONIBLE.");
            limpiar();
            buscar();
        }));
        btnActualizar.addActionListener(e -> ejecutar(() -> {
            controller.actualizar(leerFormulario());
            Dialogos.info(ventana, "Equipo actualizado correctamente.");
            limpiar();
            buscar();
        }));
        btnGenerarQr.addActionListener(e -> ejecutar(this::generarQr));
        btnGuardarQr.addActionListener(e -> ejecutar(this::guardarQr));
        btnVerHistorial.addActionListener(e -> ejecutar(() -> {
            String codigo = txtCodigoInventario.getText().trim();
            TablaUtil.mostrarEnDialogo(ventana, "Historial del equipo " + codigo, controller.historial(codigo));
        }));
        btnBuscar.addActionListener(e -> ejecutar(this::buscar));
        txtBuscar.addActionListener(e -> btnBuscar.doClick());
        tblEquipos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccionado();
            }
        });

        ejecutar(() -> {
            cargarCombos();
            buscar();
        });
        limpiar();
    }

    public void mostrar() {
        ventana.setVisible(true);
    }

    private void cargarCombos() {
        DefaultComboBoxModel<Object> tipos = new DefaultComboBoxModel<>();
        controller.tiposActivos().forEach(tipos::addElement);
        cmbTipoEquipo.setModel(tipos);

        List<Laboratorio> laboratorios = controller.laboratoriosActivos();
        DefaultComboBoxModel<Object> labs = new DefaultComboBoxModel<>();
        laboratorios.forEach(labs::addElement);
        cmbLaboratorio.setModel(labs);

        DefaultComboBoxModel<Object> filtroLab = new DefaultComboBoxModel<>();
        filtroLab.addElement(TODOS);
        laboratorios.forEach(filtroLab::addElement);
        cmbFiltroLaboratorio.setModel(filtroLab);
    }

    private void buscar() {
        Object estado = cmbFiltroEstado.getSelectedItem();
        Object lab = cmbFiltroLaboratorio.getSelectedItem();
        mostrarEnTabla(controller.listar(txtBuscar.getText(),
                TODOS.equals(estado) || estado == null ? null : EstadoEquipo.valueOf(estado.toString()),
                lab instanceof Laboratorio l ? l.getIdLaboratorio() : null));
    }

    private Equipo leerFormulario() {
        Equipo e = new Equipo();
        e.setIdEquipo(txtId.getText().isBlank() ? 0 : Integer.parseInt(txtId.getText()));
        e.setCodigoInventario(txtCodigoInventario.getText());
        e.setNombre(txtNombre.getText());
        e.setTipoEquipo((TipoEquipo) cmbTipoEquipo.getSelectedItem());
        e.setMarca(txtMarca.getText());
        e.setModelo(txtModelo.getText());
        e.setNumeroSerie(txtNumeroSerie.getText());
        e.setLaboratorio((Laboratorio) cmbLaboratorio.getSelectedItem());
        e.setObservaciones(txaObservaciones.getText());
        return e;
    }

    private void mostrarEnTabla(List<Equipo> equipos) {
        equiposEnTabla = equipos;
        modeloTabla.setRowCount(0);
        for (Equipo e : equipos) {
            modeloTabla.addRow(new Object[]{e.getIdEquipo(), e.getCodigoInventario(), e.getNombre(),
                    e.getTipoEquipo().getNombre(), e.getMarca(), e.getModelo(), e.getNumeroSerie(),
                    e.getLaboratorio().getCodigo(), e.getEstado()});
        }
    }

    private void cargarSeleccionado() {
        int fila = tblEquipos.getSelectedRow();
        if (fila < 0) {
            return;
        }
        Equipo e = equiposEnTabla.get(fila);
        txtId.setText(String.valueOf(e.getIdEquipo()));
        txtCodigoInventario.setText(e.getCodigoInventario());
        txtNombre.setText(e.getNombre());
        txtMarca.setText(e.getMarca());
        txtModelo.setText(e.getModelo());
        txtNumeroSerie.setText(e.getNumeroSerie() == null ? "" : e.getNumeroSerie());
        txaObservaciones.setText(e.getObservaciones() == null ? "" : e.getObservaciones());
        cmbEstado.setSelectedItem(e.getEstado().name());
        seleccionarTipo(e.getTipoEquipo());
        seleccionarLaboratorio(e.getLaboratorio());
        limpiarQr();
    }

    // Busca en el combo el tipo con el mismo Id (si está INACTIVO se agrega para poder verlo)
    private void seleccionarTipo(TipoEquipo tipo) {
        DefaultComboBoxModel<Object> modelo = (DefaultComboBoxModel<Object>) cmbTipoEquipo.getModel();
        for (int i = 0; i < modelo.getSize(); i++) {
            if (((TipoEquipo) modelo.getElementAt(i)).getIdTipoEquipo() == tipo.getIdTipoEquipo()) {
                cmbTipoEquipo.setSelectedIndex(i);
                return;
            }
        }
        modelo.addElement(tipo);
        cmbTipoEquipo.setSelectedItem(tipo);
    }

    private void seleccionarLaboratorio(Laboratorio lab) {
        DefaultComboBoxModel<Object> modelo = (DefaultComboBoxModel<Object>) cmbLaboratorio.getModel();
        for (int i = 0; i < modelo.getSize(); i++) {
            if (((Laboratorio) modelo.getElementAt(i)).getIdLaboratorio() == lab.getIdLaboratorio()) {
                cmbLaboratorio.setSelectedIndex(i);
                return;
            }
        }
        modelo.addElement(lab);
        cmbLaboratorio.setSelectedItem(lab);
    }

    private void generarQr() {
        if (txtId.getText().isBlank()) {
            throw new BusinessException("Seleccione en la tabla un equipo ya guardado para generar su QR.");
        }
        qrActual = controller.generarQr(txtCodigoInventario.getText(), TAMANO_QR);
        lblCodigoQr.setText("");
        lblCodigoQr.setIcon(new ImageIcon(qrActual));
    }

    private void guardarQr() {
        if (qrActual == null) {
            throw new BusinessException("Primero presione \"Generar QR\".");
        }
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new File("QR-" + txtCodigoInventario.getText().trim() + ".png"));
        if (selector.showSaveDialog(ventana) == JFileChooser.APPROVE_OPTION) {
            File archivo = selector.getSelectedFile();
            if (!archivo.getName().toLowerCase().endsWith(".png")) {
                archivo = new File(archivo.getAbsolutePath() + ".png");
            }
            controller.guardarQr(qrActual, archivo);
            Dialogos.info(ventana, "QR guardado en " + archivo.getAbsolutePath());
        }
    }

    private void limpiarQr() {
        qrActual = null;
        lblCodigoQr.setIcon(null);
        lblCodigoQr.setText("(sin QR)");
    }

    private void limpiar() {
        tblEquipos.clearSelection();
        txtId.setText("");
        txtCodigoInventario.setText("");
        txtNombre.setText("");
        txtMarca.setText("");
        txtModelo.setText("");
        txtNumeroSerie.setText("");
        txaObservaciones.setText("");
        cmbEstado.setSelectedItem(EstadoEquipo.DISPONIBLE.name());   // todo equipo nuevo nace DISPONIBLE
        if (cmbTipoEquipo.getItemCount() > 0) {
            cmbTipoEquipo.setSelectedIndex(0);
        }
        if (cmbLaboratorio.getItemCount() > 0) {
            cmbLaboratorio.setSelectedIndex(0);
        }
        limpiarQr();
        txtCodigoInventario.requestFocus();
    }

    private void ejecutar(Runnable accion) {
        Dialogos.ejecutar(ventana, accion);
    }
}
