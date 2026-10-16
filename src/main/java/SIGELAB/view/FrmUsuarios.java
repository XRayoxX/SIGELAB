package SIGELAB.view;

import SIGELAB.util.TablaUtil;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FrmUsuarios {

    // Componentes vinculados desde FrmUsuarios.form
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JTextField txtId;
    private JTextField txtUsuario;
    private JTextField txtNombre;
    private JTextField txtApellido;
    private JTextField txtCorreo;
    private JComboBox cmbRol;
    private JPasswordField pwdContrasena;
    private JPasswordField pwdConfirmarContrasena;
    private JComboBox cmbEstado;
    private JTextField txtRutaFotoRostro;
    private JButton btnSeleccionarFoto;
    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton btnLimpiar;
    private JButton btnRegresar;
    private JTextField txtBuscar;
    private JButton btnBuscar;
    private JTable tblUsuarios;

    private final JFrame ventana;

    public FrmUsuarios(JFrame menuPrincipal) {
        ventana = new JFrame("SIGELAB - Gestión de Usuarios");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Encabezados de las tablas (los datos se cargarán desde el Controller)
        TablaUtil.configurar(tblUsuarios, "ID", "Usuario", "Nombre", "Apellido", "Correo", "Rol", "Estado");

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
