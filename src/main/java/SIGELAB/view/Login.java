package SIGELAB.view;

import SIGELAB.controller.LoginController;
import SIGELAB.util.BusinessException;
import SIGELAB.util.DataAccessException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

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
        aplicarDisenoModerno();
        ventana = new JFrame("SIGELAB - Iniciar sesión");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.pack();
        ventana.setMinimumSize(new Dimension(660, 600));
        ventana.setSize(800, 680);
        ventana.setLocationRelativeTo(null);

        btnIngresar.addActionListener(e -> ingresar());
        pwdContrasena.addActionListener(e -> ingresar());   // Enter en la contraseña también ingresa
        btnSalir.addActionListener(e -> System.exit(0));
    }

    private void aplicarDisenoModerno() {
        Color azulOscuro = new Color(16, 45, 70);
        Color azul = new Color(28, 114, 151);
        Color fondo = new Color(246, 249, 253);

        // Los campos y botones siguen vinculados al archivo Login.form.
        // Reorganizamos visualmente sus componentes sin alterar LoginController.
        panelPrincipal.removeAll();
        panelPrincipal.setLayout(new BorderLayout());
        panelPrincipal.setBackground(fondo);

        JLabel encabezado = new JLabel("▣  SIGELAB   /   INSTITUTIONAL CORE LABORATORY SUITE");
        encabezado.setForeground(azulOscuro);
        encabezado.setFont(new Font("SansSerif", Font.BOLD, 12));
        encabezado.setBorder(new EmptyBorder(25, 30, 8, 30));
        panelPrincipal.add(encabezado, BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        JPanel columna = new JPanel();
        columna.setLayout(new BoxLayout(columna, BoxLayout.Y_AXIS));
        columna.setOpaque(false);

        JLabel icono = new JLabel("◈", SwingConstants.CENTER);
        icono.setForeground(azul);
        icono.setFont(new Font("SansSerif", Font.BOLD, 42));
        icono.setAlignmentX(Component.CENTER_ALIGNMENT);
        columna.add(icono);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 30));
        lblTitulo.setForeground(azulOscuro);
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        columna.add(lblTitulo);
        JLabel subtitulo = new JLabel("Sistema de Gestión y Control de Laboratorios Universitarios");
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subtitulo.setForeground(new Color(95, 110, 125));
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        columna.add(subtitulo);
        columna.add(Box.createVerticalStrut(20));

        JPanel tarjeta = new JPanel();
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBackground(Color.WHITE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 232, 240), 1, true),
                new EmptyBorder(24, 28, 22, 28)));
        tarjeta.setMaximumSize(new Dimension(480, 500));
        tarjeta.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titulo = new JLabel("Credenciales de acceso     ·     AUTH_STEP: 01/02");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 14));
        titulo.setForeground(azulOscuro);
        tarjeta.add(titulo);
        tarjeta.add(Box.createVerticalStrut(20));
        tarjeta.add(campoEtiqueta("Usuario o código institucional", azulOscuro));
        tarjeta.add(Box.createVerticalStrut(7));
        estilizarCampo(txtUsuario);
        tarjeta.add(txtUsuario);
        tarjeta.add(Box.createVerticalStrut(17));
        tarjeta.add(campoEtiqueta("Contraseña", azulOscuro));
        tarjeta.add(Box.createVerticalStrut(7));
        estilizarCampo(pwdContrasena);
        JPanel filaClave = new JPanel(new BorderLayout(8, 0));
        filaClave.setOpaque(false);
        filaClave.setAlignmentX(Component.LEFT_ALIGNMENT);
        filaClave.setMaximumSize(new Dimension(420, 43));
        filaClave.add(pwdContrasena, BorderLayout.CENTER);
        char caracterOriginal = pwdContrasena.getEchoChar();
        JButton verClave = new JButton("Mostrar");
        verClave.setBorderPainted(false);
        verClave.setFocusPainted(false);
        verClave.setBackground(Color.WHITE);
        verClave.setForeground(azul);
        verClave.addActionListener(e -> {
            boolean oculta = pwdContrasena.getEchoChar() != (char) 0;
            pwdContrasena.setEchoChar(oculta ? (char) 0 : caracterOriginal);
            verClave.setText(oculta ? "Ocultar" : "Mostrar");
        });
        filaClave.add(verClave, BorderLayout.EAST);
        tarjeta.add(filaClave);
        tarjeta.add(Box.createVerticalStrut(20));

        JPanel facial = new JPanel();
        facial.setLayout(new BoxLayout(facial, BoxLayout.Y_AXIS));
        facial.setBackground(new Color(238, 246, 255));
        facial.setBorder(new EmptyBorder(14, 14, 14, 14));
        facial.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tituloFacial = campoEtiqueta("◉  Segundo factor de autenticación", azulOscuro);
        facial.add(tituloFacial);
        facial.add(Box.createVerticalStrut(8));
        lblEstadoFacial.setText("Reconocimiento facial: integración pendiente");
        lblEstadoFacial.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblEstadoFacial.setForeground(new Color(84, 105, 123));
        facial.add(lblEstadoFacial);
        tarjeta.add(facial);
        tarjeta.add(Box.createVerticalStrut(20));

        JPanel botones = new JPanel(new BorderLayout(10, 0));
        botones.setOpaque(false);
        botones.setAlignmentX(Component.LEFT_ALIGNMENT);
        botones.setMaximumSize(new Dimension(420, 46));
        btnIngresar.setText("➜  Ingresar al sistema");
        btnIngresar.setBackground(azulOscuro);
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnIngresar.setFocusPainted(false);
        btnIngresar.setPreferredSize(new Dimension(300, 44));
        btnSalir.setBackground(new Color(231, 236, 243));
        btnSalir.setForeground(azulOscuro);
        btnSalir.setFocusPainted(false);
        botones.add(btnIngresar, BorderLayout.CENTER);
        botones.add(btnSalir, BorderLayout.EAST);
        tarjeta.add(botones);
        tarjeta.add(Box.createVerticalStrut(15));
        JLabel seguridad = new JLabel("Acceso institucional protegido  ·  SIGELAB Desktop");
        seguridad.setForeground(new Color(100, 113, 128));
        seguridad.setFont(new Font("SansSerif", Font.PLAIN, 10));
        seguridad.setAlignmentX(Component.CENTER_ALIGNMENT);
        tarjeta.add(seguridad);
        columna.add(tarjeta);
        centro.add(columna);
        panelPrincipal.add(centro, BorderLayout.CENTER);

        JLabel pie = new JLabel("SIGELAB   •   Sistema de gestión de laboratorios", SwingConstants.CENTER);
        pie.setForeground(new Color(100, 113, 128));
        pie.setBorder(new EmptyBorder(12, 0, 20, 0));
        panelPrincipal.add(pie, BorderLayout.SOUTH);
    }

    private static JLabel campoEtiqueta(String texto, Color color) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(new Font("SansSerif", Font.BOLD, 12));
        etiqueta.setForeground(color);
        etiqueta.setAlignmentX(Component.LEFT_ALIGNMENT);
        return etiqueta;
    }

    private static void estilizarCampo(JTextField campo) {
        campo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        campo.setBackground(Color.WHITE);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(211, 222, 235), 1, true),
                new EmptyBorder(8, 11, 8, 11)));
        campo.setPreferredSize(new Dimension(400, 42));
        campo.setMaximumSize(new Dimension(420, 42));
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
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
