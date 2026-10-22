package SIGELAB.view;

import SIGELAB.controller.DashboardController;
import SIGELAB.controller.LoginController;
import SIGELAB.enums.RolUsuario;
import SIGELAB.model.ResumenDashboard;
import SIGELAB.model.Usuario;
import SIGELAB.util.Dialogos;
import SIGELAB.util.Sesion;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class FrmPrincipal {

    // Componentes vinculados desde FrmPrincipal.form
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JLabel lblUsuarioActual;
    private JLabel lblTotalEquipos;
    private JLabel lblDisponibles;
    private JLabel lblPrestados;
    private JLabel lblDanados;
    private JLabel lblMantenimiento;
    private JLabel lblBaja;
    private JLabel lblPrestamosActivos;
    private JLabel lblPrestamosVencidos;
    private JButton btnPrestamos;
    private JButton btnDevoluciones;
    private JButton btnEquipos;
    private JButton btnTiposEquipo;
    private JButton btnLaboratorios;
    private JButton btnSolicitantes;
    private JButton btnMantenimiento;
    private JButton btnReportes;
    private JButton btnUsuarios;
    private JButton btnAuditoria;
    private JButton btnActualizarResumen;
    private JButton btnCerrarSesion;
    private JButton btnSalir;

    private final JFrame ventana;
    private final DashboardController dashboard = new DashboardController();

    public FrmPrincipal() {
        ventana = new JFrame("SIGELAB - Menú principal");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Usuario usuario = Sesion.getUsuarioActual();
        lblUsuarioActual.setText("Usuario: " + usuario.getNombreCompleto()
                + " (" + usuario.getNombreUsuario() + ") - Rol: " + usuario.getRol());
        adaptarMenuAlRol();

        ventana.pack();
        ventana.setLocationRelativeTo(null);

        // Dashboard: se recarga cada vez que el menú vuelve a mostrarse (al regresar de otro formulario)
        ventana.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                cargarResumen();
            }
        });
        btnActualizarResumen.addActionListener(e -> cargarResumen());

        // Cada botón oculta el menú y abre su formulario; al regresar, el menú vuelve a aparecer
        btnPrestamos.addActionListener(e -> abrir(new FrmPrestamos(ventana)::mostrar));
        btnDevoluciones.addActionListener(e -> abrir(new FrmDevoluciones(ventana)::mostrar));
        btnEquipos.addActionListener(e -> abrir(new FrmEquipos(ventana)::mostrar));
        btnTiposEquipo.addActionListener(e -> abrir(new FrmTiposEquipo(ventana)::mostrar));
        btnLaboratorios.addActionListener(e -> abrir(new FrmLaboratorios(ventana)::mostrar));
        btnSolicitantes.addActionListener(e -> abrir(new FrmSolicitantes(ventana)::mostrar));
        btnMantenimiento.addActionListener(e -> abrir(new FrmMantenimiento(ventana)::mostrar));
        btnReportes.addActionListener(e -> abrir(new FrmReportes(ventana)::mostrar));
        btnUsuarios.addActionListener(e -> abrir(new FrmUsuarios(ventana)::mostrar));
        btnAuditoria.addActionListener(e -> abrir(new FrmAuditoria(ventana)::mostrar));

        btnCerrarSesion.addActionListener(e -> {
            new LoginController().cerrarSesion();
            ventana.dispose();
            new Login().mostrar();
        });
        btnSalir.addActionListener(e -> System.exit(0));
    }

    /*
     * ADMIN: todo. ENCARGADO: equipos, préstamos, devoluciones, mantenimiento y consultas.
     * DOCENTE: solo consultas. Ocultar botones es solo visual; los Service también deben validar permisos.
     */
    private void adaptarMenuAlRol() {
        boolean admin = Sesion.tieneRol(RolUsuario.ADMIN);
        boolean operador = Sesion.tieneRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);

        btnUsuarios.setVisible(admin);
        btnAuditoria.setVisible(admin);
        btnTiposEquipo.setVisible(admin);
        btnLaboratorios.setVisible(admin);

        btnPrestamos.setVisible(operador);
        btnDevoluciones.setVisible(operador);
        btnEquipos.setVisible(operador);
        btnSolicitantes.setVisible(operador);
        btnMantenimiento.setVisible(operador);

        btnReportes.setVisible(true);
    }

    private void cargarResumen() {
        Dialogos.ejecutar(ventana, () -> {
            ResumenDashboard r = dashboard.resumen();
            lblTotalEquipos.setText(String.valueOf(r.totalEquipos()));
            lblDisponibles.setText(String.valueOf(r.disponibles()));
            lblPrestados.setText(String.valueOf(r.prestados()));
            lblDanados.setText(String.valueOf(r.danados()));
            lblMantenimiento.setText(String.valueOf(r.enMantenimiento()));
            lblBaja.setText(String.valueOf(r.deBaja()));
            lblPrestamosActivos.setText(String.valueOf(r.prestamosActivos()));
            lblPrestamosVencidos.setText(String.valueOf(r.prestamosVencidos()));
        });
    }

    private void abrir(Runnable mostrarFormulario) {
        ventana.setVisible(false);
        mostrarFormulario.run();
    }

    public void mostrar() {
        ventana.setVisible(true);
    }
}
