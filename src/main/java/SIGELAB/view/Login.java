
package SIGELAB.view;

import SIGELAB.controller.LoginController;
import SIGELAB.util.BusinessException;
import SIGELAB.util.DataAccessException;

import javax.swing.*;
import java.util.Arrays;

public class Login {

    // ========================================
    // COMPONENTES VINCULADOS A LOGIN.FORM
    // ========================================

    private JPanel panelPrincipal;

    private JPanel panelCredenciales;
    private JPanel panelFacial;
    private JPanel panelBotones;

    private JLabel lblEncabezado;
    private JLabel lblTitulo;
    private JLabel lblSubtitulo;

    private JLabel lblUsuario;
    private JLabel lblContrasena;
    private JLabel lblAyuda;

    private JLabel lblFacialTitulo;
    private JLabel lblEstadoFacial;
    private JLabel lblPiePagina;

    private JTextField txtUsuario;
    private JPasswordField pwdContrasena;

    private JCheckBox chkMostrarContrasena;

    private JButton btnIngresar;
    private JButton btnSalir;

    // ========================================
    // CONTROLADOR Y VENTANA
    // ========================================

    private final JFrame ventana;

    private final LoginController controller =
            new LoginController();

    // ========================================
    // CONSTRUCTOR
    // ========================================

    public Login() {

        ventana = new JFrame(
                "SIGELAB - Inicio de sesión"
        );

        ventana.setContentPane(panelPrincipal);

        ventana.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        ventana.pack();

        ventana.setMinimumSize(
                ventana.getSize()
        );

        ventana.setLocationRelativeTo(null);

        configurarEventos();

        ventana.getRootPane().setDefaultButton(
                btnIngresar
        );
    }

    // ========================================
    // EVENTOS
    // ========================================

    private void configurarEventos() {

        btnIngresar.addActionListener(
                e -> ingresar()
        );

        pwdContrasena.addActionListener(
                e -> ingresar()
        );

        btnSalir.addActionListener(
                e -> ventana.dispose()
        );

        configurarMostrarContrasena();
    }

    // ========================================
    // MOSTRAR / OCULTAR CONTRASEÑA
    // ========================================

    private void configurarMostrarContrasena() {

        char caracterOriginal =
                pwdContrasena.getEchoChar();

        chkMostrarContrasena.addActionListener(e -> {

            if (chkMostrarContrasena.isSelected()) {

                pwdContrasena.setEchoChar(
                        (char) 0
                );

            } else {

                pwdContrasena.setEchoChar(
                        caracterOriginal
                );
            }
        });
    }

    // ========================================
    // INICIO DE SESIÓN
    // ========================================

    private void ingresar() {

        String nombreUsuario =
                txtUsuario.getText().trim();

        char[] contrasena =
                pwdContrasena.getPassword();

        try {

            controller.iniciarSesion(
                    nombreUsuario,
                    contrasena
            );

            new FrmPrincipal().mostrar();

            ventana.dispose();

        } catch (BusinessException ex) {

            pwdContrasena.setText("");

            JOptionPane.showMessageDialog(
                    ventana,
                    ex.getMessage(),
                    "Inicio de sesión",
                    JOptionPane.WARNING_MESSAGE
            );

        } catch (DataAccessException ex) {

            ex.printStackTrace();

            JOptionPane.showMessageDialog(
                    ventana,
                    ex.getMessage(),
                    "Error de base de datos",
                    JOptionPane.ERROR_MESSAGE
            );

        } finally {

            Arrays.fill(contrasena, '\0');
        }
    }

    // ========================================
    // MOSTRAR VENTANA
    // ========================================

    public void mostrar() {

        ventana.setVisible(true);
    }
}
