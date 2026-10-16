package SIGELAB.view;

import SIGELAB.util.TablaUtil;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FrmMantenimiento {

    // Componentes vinculados desde FrmMantenimiento.form
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
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

    public FrmMantenimiento(JFrame menuPrincipal) {
        ventana = new JFrame("SIGELAB - Mantenimiento de Equipos");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Encabezados de las tablas (los datos se cargarán desde el Controller)
        TablaUtil.configurar(tblMantenimientos, "ID", "Código", "Equipo", "Tipo", "Responsable", "Fecha ingreso", "Fecha salida", "Resultado", "Estado");

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
    }

    public void mostrar() {
        ventana.setVisible(true);
    }
}
