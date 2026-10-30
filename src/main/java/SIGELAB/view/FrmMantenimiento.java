package SIGELAB.view;

import SIGELAB.controller.MantenimientoController;
import SIGELAB.enums.EstadoEquipo;
import SIGELAB.enums.EstadoMantenimiento;
import SIGELAB.enums.ResultadoMantenimiento;
import SIGELAB.enums.TipoMantenimiento;
import SIGELAB.model.Equipo;
import SIGELAB.model.Mantenimiento;
import SIGELAB.util.Dialogos;
import SIGELAB.util.EstiloFormulario;
import SIGELAB.util.Fechas;
import SIGELAB.util.TablaUtil;
import SIGELAB.util.TemaSIGELAB;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.util.List;

public class FrmMantenimiento {

    // Componentes vinculados desde FrmMantenimiento.form
    private JPanel panelPrincipal;
    private JScrollPane scrollContenido;
    private JPanel cardIngreso;
    private JPanel cardCierre;
    private JPanel cardHistorial;
    private JLabel lblTitulo;
    private JLabel lblSubtitulo;
    private JLabel lblSeccionIngreso;
    private JLabel lblSeccionCierre;
    private JLabel lblSeccionHistorial;
    private JLabel lblIndicaciones;
    private JButton btnModoClaro;
    private JButton btnModoOscuro;
    private JTextField txtId;
    private JComboBox cmbEquipo;
    private JTextField txtEstadoEquipo;
    private JComboBox cmbTipoMantenimiento;
    private JTextField txtResponsable;
    private JTextField txtFechaIngreso;
    private JComboBox cmbEstado;
    private JTextArea txaDescripcion;
    private JTextField txtFechaSalida;
    private JComboBox cmbResultado;
    private JTextArea txaObservaciones;
    private JButton btnNuevo;
    private JButton btnEnviarMantenimiento;
    private JButton btnFinalizar;
    private JButton btnLimpiar;
    private JButton btnRegresar;
    private JTextField txtBuscar;
    private JComboBox cmbFiltroEstado;
    private JButton btnBuscar;
    private JTable tblMantenimientos;

    private final JFrame ventana;
    private final DefaultTableModel modeloTabla;
    private final MantenimientoController controller = new MantenimientoController();
    private List<Mantenimiento> mantenimientosEnTabla = List.of();

    public FrmMantenimiento(JFrame menuPrincipal) {
        ventana = new JFrame("SIGELAB - Mantenimiento de Equipos");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        modeloTabla = TablaUtil.configurar(tblMantenimientos, "ID", "Código", "Equipo", "Tipo", "Responsable",
                "Fecha ingreso", "Fecha salida", "Resultado", "Estado");
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

        btnNuevo.addActionListener(e -> ejecutar(this::nuevo));
        btnLimpiar.addActionListener(e -> ejecutar(this::nuevo));
        cmbEquipo.addActionListener(e -> {
            if (cmbEquipo.getSelectedItem() instanceof Equipo equipo) {
                txtEstadoEquipo.setText(equipo.getEstado().name());
            }
        });
        btnEnviarMantenimiento.addActionListener(e -> ejecutar(this::enviar));
        btnFinalizar.addActionListener(e -> ejecutar(this::finalizar));
        btnBuscar.addActionListener(e -> ejecutar(this::buscar));
        txtBuscar.addActionListener(e -> btnBuscar.doClick());
        tblMantenimientos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccionado();
            }
        });

        ejecutar(() -> {
            nuevo();
            buscar();
        });
    }

    public void mostrar() {
        ventana.setVisible(true);
    }

    // =====================================
    // DISEÑO SIGELAB (modo claro / oscuro)
    // =====================================

    private void configurarEstilo() {
        EstiloFormulario.configurarScroll(scrollContenido);
        EstiloFormulario.tarjeta(cardIngreso, cardCierre, cardHistorial);
        EstiloFormulario.titulo(lblTitulo);
        EstiloFormulario.subtitulo(lblSubtitulo);
        EstiloFormulario.seccion(lblSeccionIngreso, lblSeccionCierre, lblSeccionHistorial);
        EstiloFormulario.indicacion(lblIndicaciones);
        EstiloFormulario.botonPrincipal(btnEnviarMantenimiento, btnFinalizar, btnBuscar);
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

    // Prepara el formulario para enviar un equipo a mantenimiento
    private void nuevo() {
        tblMantenimientos.clearSelection();
        DefaultComboBoxModel<Object> equipos = new DefaultComboBoxModel<>();
        controller.equiposParaMantenimiento().forEach(equipos::addElement);
        cmbEquipo.setModel(equipos);
        cmbEquipo.setSelectedIndex(equipos.getSize() > 0 ? 0 : -1);
        txtEstadoEquipo.setText(cmbEquipo.getSelectedItem() instanceof Equipo e ? e.getEstado().name() : "");
        txtId.setText("");
        txtResponsable.setText("");
        txtFechaIngreso.setText(Fechas.formatear(LocalDateTime.now()));
        txtFechaSalida.setText("");
        txaDescripcion.setText("");
        txaObservaciones.setText("");
        cmbTipoMantenimiento.setSelectedItem(TipoMantenimiento.CORRECTIVO.name());
        cmbEstado.setSelectedItem(EstadoMantenimiento.EN_PROCESO.name());
        cmbResultado.setSelectedItem(ResultadoMantenimiento.REPARADO.name());
        habilitarIngreso(true);
    }

    private void enviar() {
        Mantenimiento m = new Mantenimiento();
        m.setEquipo((Equipo) cmbEquipo.getSelectedItem());
        m.setTipo(TipoMantenimiento.valueOf(String.valueOf(cmbTipoMantenimiento.getSelectedItem())));
        m.setResponsable(txtResponsable.getText());
        m.setDescripcion(txaDescripcion.getText());
        int id = controller.enviar(m);
        Dialogos.info(ventana, "Mantenimiento No. " + id + " registrado.\nEl equipo "
                + m.getEquipo().getCodigoInventario() + " pasó a MANTENIMIENTO.");
        nuevo();
        buscar();
    }

    private void finalizar() {
        int id = txtId.getText().isBlank() ? 0 : Integer.parseInt(txtId.getText());
        ResultadoMantenimiento resultado = ResultadoMantenimiento.valueOf(String.valueOf(cmbResultado.getSelectedItem()));
        if (id > 0 && resultado == ResultadoMantenimiento.NO_REPARABLE
                && !Dialogos.confirmar(ventana, "El equipo quedará dado de BAJA y no podrá volver a prestarse. ¿Continuar?")) {
            return;
        }
        EstadoEquipo estadoFinal = controller.finalizar(id, resultado, txaObservaciones.getText());
        Dialogos.info(ventana, "Mantenimiento finalizado. El equipo quedó " + estadoFinal + ".");
        nuevo();
        buscar();
    }

    private void buscar() {
        Object filtro = cmbFiltroEstado.getSelectedItem();
        EstadoMantenimiento estado = filtro == null || "TODOS".equals(filtro.toString())
                ? null : EstadoMantenimiento.valueOf(filtro.toString());
        mantenimientosEnTabla = controller.listar(txtBuscar.getText(), estado);
        modeloTabla.setRowCount(0);
        for (Mantenimiento m : mantenimientosEnTabla) {
            modeloTabla.addRow(new Object[]{m.getIdMantenimiento(), m.getEquipo().getCodigoInventario(),
                    m.getEquipo().getNombre(), m.getTipo(), m.getResponsable(), Fechas.formatear(m.getFechaIngreso()),
                    Fechas.formatear(m.getFechaSalida()), m.getResultado() == null ? "" : m.getResultado(), m.getEstado()});
        }
    }

    // Muestra el mantenimiento seleccionado; si está EN_PROCESO se puede finalizar
    private void cargarSeleccionado() {
        int fila = tblMantenimientos.getSelectedRow();
        if (fila < 0) {
            return;
        }
        Mantenimiento m = mantenimientosEnTabla.get(fila);
        DefaultComboBoxModel<Object> equipo = new DefaultComboBoxModel<>();
        equipo.addElement(m.getEquipo());
        cmbEquipo.setModel(equipo);
        txtId.setText(String.valueOf(m.getIdMantenimiento()));
        txtEstadoEquipo.setText(m.getEquipo().getEstado().name());
        cmbTipoMantenimiento.setSelectedItem(m.getTipo().name());
        txtResponsable.setText(m.getResponsable());
        txtFechaIngreso.setText(Fechas.formatear(m.getFechaIngreso()));
        cmbEstado.setSelectedItem(m.getEstado().name());
        txaDescripcion.setText(m.getDescripcion());
        boolean enProceso = m.getEstado() == EstadoMantenimiento.EN_PROCESO;
        txtFechaSalida.setText(enProceso ? Fechas.formatear(LocalDateTime.now()) : Fechas.formatear(m.getFechaSalida()));
        if (m.getResultado() != null) {
            cmbResultado.setSelectedItem(m.getResultado().name());
        }
        txaObservaciones.setText(m.getObservaciones() == null ? "" : m.getObservaciones());
        habilitarIngreso(false);
        btnFinalizar.setEnabled(enProceso);
        cmbResultado.setEnabled(enProceso);
        txaObservaciones.setEditable(enProceso);
    }

    private void habilitarIngreso(boolean ingreso) {
        cmbEquipo.setEnabled(ingreso);
        cmbTipoMantenimiento.setEnabled(ingreso);
        txtResponsable.setEditable(ingreso);
        txaDescripcion.setEditable(ingreso);
        btnEnviarMantenimiento.setEnabled(ingreso);
        btnFinalizar.setEnabled(!ingreso);
        cmbResultado.setEnabled(!ingreso);
        txaObservaciones.setEditable(!ingreso);
    }

    private void ejecutar(Runnable accion) {
        Dialogos.ejecutar(ventana, accion);
    }
}
