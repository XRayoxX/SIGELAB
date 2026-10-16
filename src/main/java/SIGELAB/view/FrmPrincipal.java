package SIGELAB.view;

import javax.swing.*;

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

    public FrmPrincipal(String usuario) {
        ventana = new JFrame("SIGELAB - Menú principal");
        ventana.setContentPane(panelPrincipal);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.pack();
        ventana.setLocationRelativeTo(null);

        lblUsuarioActual.setText("Usuario: " + usuario);

        // TODO: cargar los contadores del dashboard desde la vista vw_Dashboard (vía Controller/Service)
        //       y ocultar los módulos que el rol del usuario no puede usar

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
            ventana.dispose();
            new Login().mostrar();
        });
        btnSalir.addActionListener(e -> System.exit(0));
    }

    private void abrir(Runnable mostrarFormulario) {
        ventana.setVisible(false);
        mostrarFormulario.run();
    }

    public void mostrar() {
        ventana.setVisible(true);
    }
}
