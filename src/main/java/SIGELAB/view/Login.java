package SIGELAB.view;

import javax.swing.*;

public class Login {

    // Componentes vinculados desde Login.form
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JTextField txtUsuario;
    private JPasswordField pwdContrasena;
    private JLabel lblEstadoFacial;
    private JButton btnIngresar;
    private JButton btnSalir;

    private final JFrame ventana;

    public Login() {
        // panelPrincipal y los demás componentes ya fueron creados por el GUI Designer
        ventana = new JFrame("SIGELAB - Iniciar sesión");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.pack();
        ventana.setLocationRelativeTo(null);

        btnIngresar.addActionListener(e -> {
            // TODO: validar usuario/contraseña y que el usuario esté ACTIVO (RN-01)
            // TODO: si las credenciales son válidas, llamar al servicio facial y mostrar
            //       el resultado en lblEstadoFacial; si el rostro no coincide, negar el acceso (RN-02)
            new FrmPrincipal(txtUsuario.getText()).mostrar();
            ventana.dispose();
        });
        btnSalir.addActionListener(e -> System.exit(0));
    }

    public void mostrar() {
        ventana.setVisible(true);
    }
}
