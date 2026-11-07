package SIGELAB.view;

import SIGELAB.controller.LoginController;
import SIGELAB.facial.ServicioFacial;
import SIGELAB.facial.VerificacionFacial;
import SIGELAB.model.Usuario;
import SIGELAB.util.BusinessException;
import SIGELAB.util.DataAccessException;
import SIGELAB.util.EstiloFormulario;
import SIGELAB.util.LogoSIGELAB;
import SIGELAB.util.TemaSIGELAB;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.util.Arrays;

public class Login {

    // ========================================
    // COMPONENTES DE LOGIN.FORM
    // ========================================

    private JPanel panelPrincipal;
    private JPanel panelIzquierdo;
    private JPanel panelDerecho;
    private JPanel panelSelectorTema;
    private JPanel panelFacial;

    private JLabel lblMarca;
    private JLabel lblIconoInstitucional;
    private JLabel lblMensajePrincipal;
    private JLabel lblDescripcion;
    private JLabel lblCaracteristicas;

    private JLabel lblTitulo;
    private JLabel lblSubtitulo;
    private JLabel lblUsuario;
    private JLabel lblContrasena;
    private JLabel lblFacialTitulo;
    private JLabel lblEstadoFacial;
    private JLabel lblPiePagina;

    private JTextField txtUsuario;
    private JPasswordField pwdContrasena;

    private JCheckBox chkMostrarContrasena;

    private JButton btnModoClaro;
    private JButton btnModoOscuro;
    private JButton btnIngresar;
    private JButton btnSalir;

    // ========================================
    // CONTROLADOR Y VENTANA
    // ========================================

    // ========================================
    // PALETA (la misma de FrmPrestamos: blanco / gris azulado / azul)
    // ========================================

    private static final Color OSCURO_FONDO = new Color(13, 20, 32);
    private static final Color OSCURO_TARJETA = new Color(24, 35, 51);
    private static final Color OSCURO_CAMPO = new Color(17, 28, 43);
    private static final Color OSCURO_BORDE = new Color(42, 58, 76);
    private static final Color OSCURO_TEXTO = new Color(227, 234, 242);
    private static final Color OSCURO_SECUNDARIO = new Color(151, 167, 184);

    private static final Color CLARO_FONDO = Color.WHITE;
    private static final Color CLARO_TARJETA = new Color(244, 247, 250);
    private static final Color CLARO_CAMPO = Color.WHITE;
    private static final Color CLARO_BORDE = new Color(215, 224, 233);
    private static final Color CLARO_TEXTO = new Color(35, 51, 67);
    private static final Color CLARO_SECUNDARIO = new Color(100, 117, 135);

    private static final Color AZUL_PRINCIPAL = new Color(54, 125, 170);
    // Mismo azul oscuro del fondo del logo (por si el logo no llena algún borde)
    private static final Color AZUL_LOGO = new Color(9, 27, 48);

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

        ventana.setContentPane(
                panelPrincipal
        );

        ventana.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        configurarEventos();

        mostrarLogo();

        aplicarTema();

        // Pantalla completa (maximizada), respetando la barra de tareas de Windows
        EstiloFormulario.pantallaCompleta(ventana);

        ventana.getRootPane().setDefaultButton(
                btnIngresar
        );

        // Inicializa el segundo factor sin alterar el diseño Split Screen.
        if (ServicioFacial.habilitado()) {
            lblEstadoFacial.setText(
                    "Estado: se verificará su rostro después de la contraseña"
            );
            ServicioFacial.despertar();
        } else {
            lblEstadoFacial.setText(
                    "Estado: reconocimiento facial desactivado (config.properties)"
            );
        }
    }

    // ========================================
    // LOGO
    // ========================================

    // El logo llena todo el lado izquierdo. Sus textos (SIGELAB, UMG...) ya vienen en la imagen,
    // por eso se quitan las etiquetas del .form de ese panel.
    private void mostrarLogo() {
        JComponent logo = LogoSIGELAB.panelLogo();
        if (logo == null) {
            return; // sin imagen se queda el diseño de texto del .form
        }
        panelIzquierdo.removeAll();
        panelIzquierdo.setLayout(new BorderLayout());
        panelIzquierdo.setBorder(null);
        panelIzquierdo.add(logo, BorderLayout.CENTER);
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

        configurarSelectorTema();
    }

    // ========================================
    // MOSTRAR CONTRASEÑA
    // ========================================

    private void configurarMostrarContrasena() {

        char caracterOriginal =
                pwdContrasena.getEchoChar();

        chkMostrarContrasena.addActionListener(e -> {

            boolean mostrar =
                    chkMostrarContrasena.isSelected();

            pwdContrasena.setEchoChar(
                    mostrar
                            ? (char) 0
                            : caracterOriginal
            );
        });
    }

    // ========================================
    // SELECTOR SOL Y LUNA
    // ========================================

    private void configurarSelectorTema() {

        // SOL: modo claro
        btnModoClaro.addActionListener(e -> {

            TemaSIGELAB.establecerModoOscuro(
                    false
            );

            aplicarTema();
        });

        // LUNA: modo oscuro
        btnModoOscuro.addActionListener(e -> {

            TemaSIGELAB.establecerModoOscuro(
                    true
            );

            aplicarTema();
        });
    }

    // ========================================
    // APLICAR TEMA
    // ========================================

    private void aplicarTema() {

        boolean oscuro =
                TemaSIGELAB.esOscuro();

        Color fondo =
                oscuro ? OSCURO_FONDO : CLARO_FONDO;

        Color panel =
                oscuro ? OSCURO_TARJETA : CLARO_TARJETA;

        Color texto =
                oscuro ? OSCURO_TEXTO : CLARO_TEXTO;

        Color secundario =
                oscuro ? OSCURO_SECUNDARIO : CLARO_SECUNDARIO;

        Color borde =
                oscuro ? OSCURO_BORDE : CLARO_BORDE;

        Color campo =
                oscuro ? OSCURO_CAMPO : CLARO_CAMPO;

        // ------------------------------------
        // FONDOS
        // ------------------------------------

        panelPrincipal.setBackground(fondo);

        // El lado izquierdo es el logo (siempre con su fondo azul oscuro)
        panelIzquierdo.setBackground(AZUL_LOGO);

        panelDerecho.setBackground(fondo);

        panelFacial.setBackground(panel);

        panelSelectorTema.setOpaque(false);

        // ------------------------------------
        // IDENTIDAD IZQUIERDA
        // ------------------------------------

        Color textoIzquierdo =
                oscuro
                        ? Color.WHITE
                        : new Color(17, 45, 71);

        lblMarca.setForeground(Color.WHITE);

        lblIconoInstitucional.setForeground(
                Color.WHITE
        );

        lblMensajePrincipal.setForeground(
                Color.WHITE
        );

        lblDescripcion.setForeground(
                oscuro
                        ? new Color(181, 211, 232)
                        : new Color(220, 237, 226)
        );

        lblCaracteristicas.setForeground(
                oscuro
                        ? new Color(181, 211, 232)
                        : new Color(220, 237, 226)
        );

        // ------------------------------------
        // TITULOS DE ACCESO
        // ------------------------------------

        lblTitulo.setForeground(texto);

        lblSubtitulo.setForeground(secundario);

        lblUsuario.setForeground(texto);

        lblContrasena.setForeground(texto);

        lblFacialTitulo.setForeground(texto);

        lblEstadoFacial.setForeground(secundario);

        lblPiePagina.setForeground(secundario);

        // ------------------------------------
        // CAMPOS
        // ------------------------------------

        configurarCampo(
                txtUsuario,
                campo,
                texto,
                borde
        );

        configurarCampo(
                pwdContrasena,
                campo,
                texto,
                borde
        );

        chkMostrarContrasena.setForeground(texto);
        chkMostrarContrasena.setOpaque(false);

        // ------------------------------------
        // BOTON PRINCIPAL
        // ------------------------------------

        btnIngresar.setBackground(AZUL_PRINCIPAL);
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setFocusPainted(false);
        btnIngresar.setOpaque(true);

        // ------------------------------------
        // BOTON SALIR (secundario: mismo fondo de las tarjetas, con borde)
        // ------------------------------------

        btnSalir.setBackground(panel);
        btnSalir.setForeground(texto);
        btnSalir.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(borde, 1),
                        BorderFactory.createEmptyBorder(8, 16, 8, 16)
                )
        );
        btnSalir.setFocusPainted(false);
        btnSalir.setOpaque(true);

        // ------------------------------------
        // SOL Y LUNA
        // ------------------------------------

        configurarBotonTema(
                btnModoClaro,
                !oscuro
        );

        configurarBotonTema(
                btnModoOscuro,
                oscuro
        );

        // ------------------------------------
        // FACTOR FACIAL
        // ------------------------------------

        Border linea =
                BorderFactory.createLineBorder(
                        borde,
                        1
                );

        TitledBorder bordeFacial =
                BorderFactory.createTitledBorder(
                        linea,
                        "Segundo factor de autenticación"
                );

        bordeFacial.setTitleColor(texto);

        bordeFacial.setTitleFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        12
                )
        );

        panelFacial.setBorder(bordeFacial);

        // ------------------------------------
        // ACTUALIZAR
        // ------------------------------------

        panelPrincipal.revalidate();
        panelPrincipal.repaint();
    }

    // ========================================
    // CONFIGURAR CAMPOS
    // ========================================

    private void configurarCampo(
            JTextField componente,
            Color fondo,
            Color texto,
            Color borde
    ) {

        componente.setBackground(fondo);
        componente.setForeground(texto);
        componente.setCaretColor(texto);

        componente.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                borde,
                                1
                        ),
                        BorderFactory.createEmptyBorder(
                                8, 12, 8, 12
                        )
                )
        );
    }

    // ========================================
    // CONFIGURAR BOTONES SOL Y LUNA
    // ========================================

    private void configurarBotonTema(
            JButton boton,
            boolean activo
    ) {

        boolean oscuro = TemaSIGELAB.esOscuro();

        // Activo: azul de SIGELAB. Inactivo: gris como las tarjetas.
        boton.setBackground(
                activo
                        ? AZUL_PRINCIPAL
                        : (oscuro ? OSCURO_TARJETA : CLARO_TARJETA)
        );

        boton.setForeground(
                activo
                        ? Color.WHITE
                        : (oscuro ? OSCURO_TEXTO : CLARO_TEXTO)
        );

        boton.setFocusPainted(false);

        boton.setOpaque(true);

        boton.setToolTipText(
                boton == btnModoClaro
                        ? "Activar modo claro"
                        : "Activar modo oscuro"
        );

        boton.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );
    }

    // ========================================
    // INICIO DE SESION
    // ========================================

    private void ingresar() {

        String nombreUsuario =
                txtUsuario.getText().trim();

        char[] contrasena =
                pwdContrasena.getPassword();

        try {

            // Primero comprobar credenciales y estado de la cuenta.
            Usuario usuario = controller.validarCredenciales(
                    nombreUsuario,
                    contrasena
            );

            // RN-02: segundo factor facial antes de completar la sesión.
            lblEstadoFacial.setText(
                    "Estado: verificando el rostro de "
                            + usuario.getNombreUsuario() + "..."
            );

            VerificacionFacial.Resultado resultado =
                    VerificacionFacial.verificarEnLogin(ventana, usuario);

            switch (resultado) {
                case APROBADO:
                    controller.completarLogin(usuario, resultado);
                    new FrmPrincipal().mostrar();
                    ventana.dispose();
                    return;

                case RECHAZADO:
                    controller.registrarFalloFacial(
                            usuario,
                            "El rostro no coincide o no pasó la prueba de vida"
                    );
                    lblEstadoFacial.setText(
                            "Estado: rostro no reconocido. Intente de nuevo."
                    );
                    break;

                case CANCELADO:
                    lblEstadoFacial.setText(
                            "Estado: verificación facial cancelada."
                    );
                    break;

                case NO_DISPONIBLE:
                    controller.registrarFalloFacial(
                            usuario,
                            "Servicio facial no disponible o sin configuración"
                    );

                    lblEstadoFacial.setText(
                            "Estado: servicio facial no disponible."
                    );
                    break;
            }

            pwdContrasena.setText("");

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
    // MOSTRAR LOGIN
    // ========================================

    public void mostrar() {

        ventana.setVisible(true);
    }
}
