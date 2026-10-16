package SIGELAB.view;

import SIGELAB.util.TablaUtil;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FrmAuditoria {

    // Componentes vinculados desde FrmAuditoria.form
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JComboBox cmbUsuario;
    private JComboBox cmbEntidad;
    private JComboBox cmbAccion;
    private JTextField txtFechaDesde;
    private JTextField txtFechaHasta;
    private JButton btnBuscar;
    private JButton btnLimpiar;
    private JButton btnExportar;
    private JButton btnRegresar;
    private JTable tblAuditoria;
    private JLabel lblTotalRegistros;

    private final JFrame ventana;

    public FrmAuditoria(JFrame menuPrincipal) {
        ventana = new JFrame("Auditoría del Sistema ");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Encabezados de las tablas (los datos se cargarán desde el Controller)
        TablaUtil.configurar(tblAuditoria, "ID", "Fecha/Hora", "Usuario", "Acción", "Entidad", "ID registro", "Descripción");

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
