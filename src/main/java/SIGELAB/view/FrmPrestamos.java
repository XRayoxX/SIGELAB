package SIGELAB.view;

import SIGELAB.util.TablaUtil;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FrmPrestamos {

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

    public FrmPrestamos(JFrame menuPrincipal) {
        ventana = new JFrame("SIGELAB - Préstamos de Equipo De Computo");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Encabezados de las tablas (los datos se cargarán desde el Controller)
        TablaUtil.configurar(tblDetalle, "ID", "Código largo", "Equipo", "Tipo", "Laboratorio", "Estado");
        TablaUtil.configurar(tblPrestamos, "No.", "Solicitante", "Fecha préstamo", "Fecha esperada", "Registrado por", "Estado", "Pendientes");

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
