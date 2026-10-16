package SIGELAB.view;

import SIGELAB.util.TablaUtil;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FrmSolicitantes {

    // Componentes vinculados desde FrmSolicitantes.form
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
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

    public FrmSolicitantes(JFrame menuPrincipal) {
        ventana = new JFrame("SIGELAB - Gestión de Solicitantes");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Encabezados de las tablas (los datos se cargarán desde el Controller)
        TablaUtil.configurar(tblSolicitantes, "ID", "Carné", "Nombre", "Apellido", "Tipo", "Correo", "Teléfono", "Estado");

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
