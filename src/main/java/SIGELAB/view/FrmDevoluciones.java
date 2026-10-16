package SIGELAB.view;

import SIGELAB.util.TablaUtil;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

    public FrmDevoluciones(JFrame menuPrincipal) {
        ventana = new JFrame("SIGELAB - Devolución de Equipo");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Encabezados de las tablas (los datos se cargarán desde el Controller)
        TablaUtil.configurar(tblPrestamosActivos, "No.", "Solicitante", "Fecha préstamo", "Fecha esperada", "Pendientes", "Vencido");
        TablaUtil.configurar(tblEquiposPrestamo, "ID detalle", "Código", "Equipo", "Estado", "Condición", "Fecha devolución");

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
