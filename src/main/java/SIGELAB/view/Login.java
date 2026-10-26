package SIGELAB.view;

import SIGELAB.controller.LoginController;
import SIGELAB.util.BusinessException;
import SIGELAB.util.DataAccessException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class Login {

    // Componentes vinculados desde Login.form
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JTextField txtUsuario;
    private JPasswordField pwdContrasena;
    private JLabel lblEstadoFacial;
    private JButton btnIngresar;
    private JButton btnSalir;
    private JButton btnFacial;

    private final JFrame ventana;
    private final LoginController controller = new LoginController();
    private boolean autenticando;

    public Login() {
        // panelPrincipal y los demás componentes ya fueron creados por el GUI Designer
        ventana = new JFrame("SIGELAB - Iniciar sesión");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.pack();
        ventana.setLocationRelativeTo(null);
        ventana.setResizable(false);

        configurarEstilos();
        configurarEventos();
    }

    private void configurarEstilos() {
        Color verdePrincipal = new Color(0, 128, 0);
        Color verdeHover = new Color(0, 150, 0);
        Color borde = new Color(200, 200, 200);
        Color foco = new Color(46, 125, 50);

        if (btnIngresar != null) {
            estilizarBotonVerde(btnIngresar);
        }
        if (btnFacial != null) {
            estilizarBotonVerde(btnFacial);
        }
        if (btnSalir != null) {
            estilizarBotonVerde(btnSalir);
        }

        if (txtUsuario != null) {
            estilizarCampo(txtUsuario, borde, foco);
        }
        if (pwdContrasena != null) {
            estilizarCampo(pwdContrasena, borde, foco);
        }
    }

    private void estilizarBotonVerde(JButton boton) {
        Color verdePrincipal = new Color(0, 128, 0);
        Color verdeHover = new Color(0, 150, 0);
        Color verdePresionado = new Color(0, 110, 0);
        boton.setBackground(verdePrincipal);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setOpaque(true);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setBorder(new EmptyBorder(8, 16, 8, 16));
        boton.setContentAreaFilled(true);
        boton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (boton.isEnabled()) {
                    boton.setBackground(verdeHover);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (boton.isEnabled()) {
                    boton.setBackground(verdePrincipal);
                }
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (boton.isEnabled()) {
                    boton.setBackground(verdePresionado);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (boton.isEnabled()) {
                    boton.setBackground(boton.contains(e.getPoint()) ? verdeHover : verdePrincipal);
                }
            }
        });
    }

    private void estilizarBoton(JButton boton, Color fondo, Color texto) {
        boton.setBackground(fondo);
        boton.setForeground(texto);
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setOpaque(true);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setBorder(new EmptyBorder(8, 16, 8, 16));
        boton.setContentAreaFilled(true);
    }

    private void estilizarCampo(JTextField campo, Color borde, Color foco) {
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borde),
                new EmptyBorder(4, 10, 4, 10)
        ));
        campo.setFont(campo.getFont().deriveFont(12f));
        campo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                campo.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(foco),
                        new EmptyBorder(4, 10, 4, 10)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                campo.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(borde),
                        new EmptyBorder(4, 10, 4, 10)
                ));
            }
        });
    }

    private void configurarEventos() {
        if (btnIngresar != null) {
            btnIngresar.addActionListener(e -> ingresar());
        }
        if (btnFacial != null) {
            btnFacial.addActionListener(e -> ingresarPorReconocimientoFacial());
        }
        if (btnSalir != null) {
            btnSalir.addActionListener(e -> System.exit(0));
        }
        if (pwdContrasena != null) {
            pwdContrasena.addActionListener(e -> ingresar());   // Enter en la contraseña también ingresa
        }
    }

    private void ingresar() {
        if (autenticando) {
            return;
        }

        String usuario = txtUsuario.getText() == null ? "" : txtUsuario.getText().trim();
        if (usuario.isEmpty()) {
            JOptionPane.showMessageDialog(ventana, "Ingrese su usuario.", "Inicio de sesión", JOptionPane.WARNING_MESSAGE);
            txtUsuario.requestFocus();
            return;
        }
        if (pwdContrasena == null || pwdContrasena.getPassword().length == 0) {
            JOptionPane.showMessageDialog(ventana, "Ingrese su contraseña.", "Inicio de sesión", JOptionPane.WARNING_MESSAGE);
            pwdContrasena.requestFocus();
            return;
        }

        autenticando = true;
        setBotonesHabilitados(false);
        if (btnIngresar != null) {
            btnIngresar.setText("VERIFICANDO...");
        }

        try {
            controller.iniciarSesion(usuario, pwdContrasena.getPassword());
            if (lblEstadoFacial != null) {
                lblEstadoFacial.setText("Verificación facial: pendiente de validación");
                lblEstadoFacial.setForeground(new Color(108, 117, 125));
            }
            new FrmPrincipal().mostrar();
            ventana.dispose();
        } catch (BusinessException ex) {
            if (pwdContrasena != null) {
                pwdContrasena.setText("");
            }
            JOptionPane.showMessageDialog(ventana, ex.getMessage(), "Inicio de sesión", JOptionPane.WARNING_MESSAGE);
        } catch (DataAccessException ex) {
            ex.printStackTrace();   // el detalle técnico queda en consola, no en la pantalla
            JOptionPane.showMessageDialog(ventana, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        } finally {
            autenticando = false;
            setBotonesHabilitados(true);
            if (btnIngresar != null) {
                btnIngresar.setText("INGRESAR");
            }
            if (btnFacial != null) {
                btnFacial.setText("RECONOCIMIENTO FACIAL");
            }
        }
    }

    private void ingresarPorReconocimientoFacial() {
        if (autenticando) {
            return;
        }

        String usuario = txtUsuario.getText() == null ? "" : txtUsuario.getText().trim();
        if (usuario.isEmpty()) {
            JOptionPane.showMessageDialog(ventana, "Ingrese el usuario para realizar el reconocimiento facial.", "Reconocimiento Facial", JOptionPane.WARNING_MESSAGE);
            txtUsuario.requestFocus();
            return;
        }

        autenticando = true;
        setBotonesHabilitados(false);
        if (btnFacial != null) {
            btnFacial.setText("INICIANDO RECONOCIMIENTO...");
        }
        if (lblEstadoFacial != null) {
            lblEstadoFacial.setText("Iniciando reconocimiento facial...");
            lblEstadoFacial.setForeground(new Color(0, 102, 51));
        }

        try {
            controller.iniciarSesion(usuario, new char[0]);
            if (lblEstadoFacial != null) {
                lblEstadoFacial.setText("Reconocimiento facial: aprobado");
                lblEstadoFacial.setForeground(new Color(0, 102, 51));
            }
            new FrmPrincipal().mostrar();
            ventana.dispose();
        } catch (BusinessException ex) {
            if (lblEstadoFacial != null) {
                lblEstadoFacial.setText("Reconocimiento facial: rechazado");
                lblEstadoFacial.setForeground(new Color(220, 53, 69));
            }
            JOptionPane.showMessageDialog(ventana, ex.getMessage(), "Reconocimiento Facial", JOptionPane.WARNING_MESSAGE);
        } catch (DataAccessException ex) {
            ex.printStackTrace();
            if (lblEstadoFacial != null) {
                lblEstadoFacial.setText("Reconocimiento facial: error");
                lblEstadoFacial.setForeground(new Color(220, 53, 69));
            }
            JOptionPane.showMessageDialog(ventana, ex.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        } finally {
            autenticando = false;
            setBotonesHabilitados(true);
            if (btnIngresar != null) {
                btnIngresar.setText("INGRESAR");
            }
            if (btnFacial != null) {
                btnFacial.setText("RECONOCIMIENTO FACIAL");
            }
        }
    }

    private void setBotonesHabilitados(boolean habilitado) {
        if (btnIngresar != null) {
            btnIngresar.setEnabled(habilitado);
        }
        if (btnFacial != null) {
            btnFacial.setEnabled(habilitado);
        }
        if (btnSalir != null) {
            btnSalir.setEnabled(habilitado);
        }
        if (txtUsuario != null) {
            txtUsuario.setEnabled(habilitado);
        }
        if (pwdContrasena != null) {
            pwdContrasena.setEnabled(habilitado);
        }
    }

    public void mostrar() {
        ventana.setVisible(true);
    }
}
