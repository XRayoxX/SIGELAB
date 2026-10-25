
package SIGELAB.view;

import SIGELAB.controller.LoginController;
import SIGELAB.util.BusinessException;
import SIGELAB.util.DataAccessException;
import SIGELAB.util.TemaSIGELAB;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.util.Arrays;

public class Login {

    // ========================================
    // COMPONENTES DEL LOGIN.FORM
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
    private JCheckBox chkModoOscuro;

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

        // Calcular inicialmente el tamaño de los componentes.
        ventana.pack();

// Maximizar dentro del área de trabajo de Windows.
// La barra de tareas permanece visible.
        ventana.setExtendedState(JFrame.MAXIMIZED_BOTH);

        configurarEventos();

        // Recuperar tema guardado.
        chkModoOscuro.setSelected(
                TemaSIGELAB.esOscuro()
        );

        // Aplicar los colores al iniciar.
        aplicarTema();

        ventana.getRootPane().setDefaultButton(
                btnIngresar
        );
    }

    // ========================================
    // CONFIGURACIÓN DE EVENTOS
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

        configurarSelectorTema();
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
    // SELECTOR DE TEMA
    // ========================================

    private void configurarSelectorTema() {

        chkModoOscuro.addActionListener(e -> {

            boolean oscuro =
                    chkModoOscuro.isSelected();

            TemaSIGELAB.establecerModoOscuro(
                    oscuro
            );

            aplicarTema();
        });
    }

    // ========================================
    // APLICAR TEMA A LA INTERFAZ
    // ========================================

    private void aplicarTema() {

        Color fondo = TemaSIGELAB.fondo();
        Color panel = TemaSIGELAB.panel();
        Color texto = TemaSIGELAB.texto();
        Color secundario = TemaSIGELAB.textoSecundario();
        Color primario = TemaSIGELAB.primario();
        Color borde = TemaSIGELAB.borde();
        Color campo = TemaSIGELAB.campo();

        // ------------------------------------
        // PANEL PRINCIPAL
        // ------------------------------------

        panelPrincipal.setBackground(fondo);

        panelBotones.setBackground(fondo);

        // ------------------------------------
        // PANELES INTERNOS
        // ------------------------------------

        panelCredenciales.setBackground(panel);

        panelFacial.setBackground(panel);

        // ------------------------------------
        // ENCABEZADO
        // ------------------------------------

        lblEncabezado.setForeground(texto);

        lblTitulo.setForeground(texto);

        lblSubtitulo.setForeground(secundario);

        chkModoOscuro.setForeground(texto);
        chkModoOscuro.setOpaque(false);

        // ------------------------------------
        // ETIQUETAS
        // ------------------------------------

        lblUsuario.setForeground(texto);

        lblContrasena.setForeground(texto);

        lblAyuda.setForeground(secundario);

        lblFacialTitulo.setForeground(texto);

        lblEstadoFacial.setForeground(secundario);

        lblPiePagina.setForeground(secundario);

        // ------------------------------------
        // CAMPOS DE TEXTO
        // ------------------------------------

        txtUsuario.setBackground(campo);
        txtUsuario.setForeground(texto);
        txtUsuario.setCaretColor(texto);

        txtUsuario.setBorder(
                BorderFactory.createLineBorder(
                        borde,
                        1
                )
        );

        pwdContrasena.setBackground(campo);
        pwdContrasena.setForeground(texto);
        pwdContrasena.setCaretColor(texto);

        pwdContrasena.setBorder(
                BorderFactory.createLineBorder(
                        borde,
                        1
                )
        );

        // ------------------------------------
        // MOSTRAR CONTRASEÑA
        // ------------------------------------

        chkMostrarContrasena.setBackground(panel);
        chkMostrarContrasena.setForeground(texto);

        // ------------------------------------
        // BOTÓN INGRESAR
        // ------------------------------------

        btnIngresar.setBackground(primario);

        btnIngresar.setForeground(
                TemaSIGELAB.textoSobrePrimario()
        );

        btnIngresar.setFocusPainted(false);

        // ------------------------------------
        // BOTÓN SALIR
        // ------------------------------------

        btnSalir.setBackground(panel);
        btnSalir.setForeground(texto);
        btnSalir.setFocusPainted(false);

        // ------------------------------------
        // BORDES DE LOS PANELES
        // ------------------------------------

        actualizarBorde(
                panelCredenciales,
                "Credenciales de acceso"
        );

        actualizarBorde(
                panelFacial,
                "Segundo factor de autenticación"
        );

        // ------------------------------------
        // ACTUALIZAR INTERFAZ
        // ------------------------------------

        panelPrincipal.revalidate();
        panelPrincipal.repaint();
    }

    // ========================================
    // BORDES CON TÍTULO
    // ========================================

    private void actualizarBorde(
            JPanel panel,
            String titulo
    ) {

        Border linea =
                BorderFactory.createLineBorder(
                        TemaSIGELAB.borde(),
                        1
                );

        TitledBorder bordeTitulo =
                BorderFactory.createTitledBorder(
                        linea,
                        titulo
                );

        bordeTitulo.setTitleColor(
                TemaSIGELAB.texto()
        );

        bordeTitulo.setTitleFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        12
                )
        );

        panel.setBorder(bordeTitulo);
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
