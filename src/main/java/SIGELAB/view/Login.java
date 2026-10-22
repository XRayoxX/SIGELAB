package SIGELAB.view;

import SIGELAB.controller.LoginController;
import SIGELAB.util.BusinessException;
import SIGELAB.util.DataAccessException;

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
    private final LoginController controller = new LoginController();

    public Login() {
        // panelPrincipal y los demás componentes ya fueron creados por el GUI Designer
        ventana = new JFrame("SIGELAB - Iniciar sesión");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.pack();
        ventana.setLocationRelativeTo(null);

        btnIngresar.addActionListener(e -> ingresar());
        pwdContrasena.addActionListener(e -> ingresar());   // Enter en la contraseña también ingresa
        btnSalir.addActionListener(e -> System.exit(0));
    }

    private void ingresar() {
        try {
            controller.iniciarSesion(txtUsuario.getText(), pwdContrasena.getPassword());
            new FrmPrincipal().mostrar();
            ventana.dispose();
        } catch (BusinessException ex) {
            pwdContrasena.setText("");
            JOptionPane.showMessageDialog(ventana, ex.getMessage(), "Inicio de sesión", JOptionPane.WARNING_MESSAGE);
        } catch (DataAccessException ex) {
            ex.printStackTrace();   // el detalle técnico queda en consola, no en la pantalla
            JOptionPane.showMessageDialog(ventana, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void mostrar() {
        ventana.setVisible(true);
    }
}
