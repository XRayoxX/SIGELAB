package SIGELAB.view;

import SIGELAB.util.TablaUtil;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FrmEquipos {

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

    public FrmEquipos(JFrame menuPrincipal) {
        ventana = new JFrame("SIGELAB - Gestión de Equipos");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Encabezados de las tablas (los datos se cargarán desde el Controller)
        TablaUtil.configurar(tblEquipos, "ID", "Código", "Nombre", "Tipo", "Marca", "Modelo", "No. serie", "Laboratorio", "Estado");

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
