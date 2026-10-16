package SIGELAB.view;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FrmReportes {

    // Componentes vinculados desde FrmReportes.form
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JComboBox cmbTipoReporte;
    private JComboBox cmbLaboratorio;
    private JTextField txtFechaDesde;
    private JTextField txtFechaHasta;
    private JComboBox cmbEstadoEquipo;
    private JTextField txtCodigoEquipo;
    private JButton btnGenerar;
    private JButton btnExportarPdf;
    private JButton btnExportarExcel;
    private JButton btnImprimir;
    private JButton btnLimpiar;
    private JButton btnRegresar;
    private JTable tblResultados;
    private JLabel lblTotalRegistros;

    private final JFrame ventana;

    public FrmReportes(JFrame menuPrincipal) {
        ventana = new JFrame("SIGELAB - Reportes");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
